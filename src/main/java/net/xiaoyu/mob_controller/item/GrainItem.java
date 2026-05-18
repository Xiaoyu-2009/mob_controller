package net.xiaoyu.mob_controller.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.util.CustomControlHandler;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;

import javax.annotation.Nullable;
import java.util.List;

public class GrainItem extends MobControllerItem {

    public GrainItem(Properties properties) {
        super(properties);
    }

    // 对生物右键：控制逻辑（与之前相同）
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Mob mob)) {
            return InteractionResult.PASS;
        }
        Level level = player.level();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        // 如果生物有自定义规则且当前物品不匹配，禁止控制
        if (CustomControlHandler.hasCustomRule(mob) && !CustomControlHandler.isMatchingCustomItem(mob, stack.getItem())) {
            MobControlUtil.spawnControlParticles(mob, false);
            return InteractionResult.FAIL;
        }

        // 如果已经被控制，直接返回（不消耗）
        if (MobControlledData.isControlledEntity(mob)) {
            return InteractionResult.PASS;
        }

        boolean alwaysSuccess = Config.ALWAYS_SUCCESS.get();

        // 使用统一的条件检查
        if (!MobControlUtil.canBeControlled(mob, player, alwaysSuccess)) {
            MobControlUtil.spawnControlParticles(mob, false);
            return InteractionResult.FAIL;
        }

        // 概率判定（成功或概率失败都会消耗物品）
        float controlChance = alwaysSuccess ? 1.0f : MobControlUtil.calculateControlChance(mob);
        boolean success = level.random.nextFloat() <= controlChance;
        if (success) {
            mob.setTarget(null);
            MobControlUtil.performControlMob(player, mob);
            MobControlUtil.showMessageToPlayer(player, mob.getDisplayName(), "mob_controller.mode.follow", new Object[]{}, ChatFormatting.GOLD);
            MobControlUtil.spawnControlParticles(mob, true);
        } else {
            MobControlUtil.spawnControlParticles(mob, false);
        }

        // 消耗物品（创造模式不消耗）
        if (!player.isCreative()) {
            stack.shrink(1);
        }

        return success ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    // 禁止对空气右键时饮用（直接返回 PASS，不触发使用动画）
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    // 覆写饮用动画相关方法，确保不会被调用
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 0;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("mob_controller.tooltip.grain").withStyle(ChatFormatting.AQUA));
    }
}