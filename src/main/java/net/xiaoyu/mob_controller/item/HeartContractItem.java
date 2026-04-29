package net.xiaoyu.mob_controller.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.xiaoyu.mob_controller.util.MobControlledData;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class HeartContractItem extends Item {

    public HeartContractItem(Properties properties) {
        super(properties);
    }

    // 右键生物解除控制
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Mob mob)) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;

        if (!MobControlledData.isControlledEntity(mob)) return InteractionResult.PASS;
        UUID controllerUUID = MobControlledData.getControllerUUID(mob);
        if (controllerUUID == null || !controllerUUID.equals(player.getUUID())) return InteractionResult.FAIL;
        if (MobControlledData.isSummoned(mob)) return InteractionResult.FAIL;

        mob.setTarget(null);
        MobControlledData.releaseControl(mob);
        mob.persistenceRequired = false;
        return InteractionResult.SUCCESS;
    }

    // 饮用动作（对空气右键长按）
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!level.isClientSide && livingEntity instanceof Player player) {
            // 恢复 1 点饥饿值
            player.getFoodData().eat(1, 0.1F);
            // 施加负面效果（10 秒 = 200 tick）
            int duration = 200;
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, duration, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, duration, 0));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration, 0));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 4));
            // 生存/冒险模式消耗一个物品
            if (!player.isCreative()) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    return new ItemStack(Items.BOWL);
                } else {
                    if (!player.getInventory().add(new ItemStack(Items.BOWL))) {
                        player.drop(new ItemStack(Items.BOWL), false);
                    }
                }
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("mob_controller.tooltip.heartcontractitem").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("mob_controller.tooltip.heart_contract_ext").withStyle(ChatFormatting.DARK_PURPLE));
    }
}