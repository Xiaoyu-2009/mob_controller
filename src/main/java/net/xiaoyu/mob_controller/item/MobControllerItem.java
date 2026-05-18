package net.xiaoyu.mob_controller.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.util.CustomControlHandler;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;

import javax.annotation.Nullable;
import java.util.List;

public class MobControllerItem extends Item {

    private static final int CONTROL_COMMAND_RANGE = 32;
    private static final int GLOWING_DURATION_TICKS = 100;

    public MobControllerItem(Properties properties) {
        super(properties);
    }

    // 批量控制令
    public static int applyControlCommand(Player player, MobControlledData.ControlMode mode) {
        if (player.level().isClientSide) return 0;
        AABB area = player.getBoundingBox().inflate(CONTROL_COMMAND_RANGE);
        List<Mob> controlledMobs = player.level().getEntitiesOfClass(
                Mob.class, area, mob ->
                        MobControlledData.isControlledEntity(mob) && player.getUUID().equals(MobControlledData.getControllerUUID(mob))
        );
        for (Mob mob : controlledMobs) {
            MobControlledData.setControlMode(mob, mode);
            mob.setTarget(null);
            MobControlledData.clearSystemAttack(mob);
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOWING_DURATION_TICKS));
        }
        return controlledMobs.size();
    }

    // 对生物右键 -> 控制尝试，不触发饮用，只播放挥臂动画
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof Mob mob) {
            // 客户端：立即播放挥臂动画，表示“使用”了物品
            if (player.level().isClientSide) {
                player.swing(hand);
                return InteractionResult.SUCCESS;
            }

            // 如果生物有自定义规则且当前物品不匹配，禁止控制
            if (CustomControlHandler.hasCustomRule(mob) && !CustomControlHandler.isMatchingCustomItem(mob, stack.getItem())) {
                MobControlUtil.spawnControlParticles(mob, false);
                return InteractionResult.FAIL;
            }

            // 服务端逻辑
            Level level = player.level();
            if (MobControlledData.isControlledEntity(mob)) return InteractionResult.PASS;

            boolean alwaysSuccess = Config.ALWAYS_SUCCESS.get();
            // 使用统一的条件检查
            if (!MobControlUtil.canBeControlled(mob, player, alwaysSuccess)) {
                MobControlUtil.spawnControlParticles(mob, false);
                return InteractionResult.FAIL;
            }

            float controlChance = alwaysSuccess ? 1.0f : MobControlUtil.calculateControlChance(mob);
            if (level.random.nextFloat() <= controlChance) {
                mob.setTarget(null);
                MobControlUtil.performControlMob(player, mob);
                MobControlUtil.showMessageToPlayer(player, mob.getDisplayName(), "mob_controller.mode.follow", new Object[]{}, ChatFormatting.GOLD);
                MobControlUtil.spawnControlParticles(mob, true);
                return InteractionResult.SUCCESS;
            } else {
                MobControlUtil.spawnControlParticles(mob, false);
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    // 对空气右键长按 -> 饮用（完整动画 + 效果）
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
            player.getFoodData().eat(8, 1.6F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("mob_controller.tooltip.mob_controller").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("mob_controller.tooltip.mob_controller.desc1").withStyle(ChatFormatting.GOLD));
    }
}