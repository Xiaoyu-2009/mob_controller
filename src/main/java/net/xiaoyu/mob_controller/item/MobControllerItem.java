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
                if (!player.level().isClientSide) {
                    spawnParticles(mob, false);
                }
                return InteractionResult.FAIL;
            }

            // 服务端逻辑
            Level level = player.level();
            if (MobControlledData.isControlledEntity(mob)) return InteractionResult.PASS;

            boolean alwaysSuccess = Config.ALWAYS_SUCCESS.get();
            // 攻击力限制
            if (!alwaysSuccess) {
                AttributeInstance attackAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
                double attackDamage = attackAttr != null ? attackAttr.getValue() : 0.0;
                if (attackDamage >= Config.ATTACK_LIMIT.get()) {
                    spawnParticles(mob, false);
                    return InteractionResult.FAIL;
                }
            }
            // 生命上限限制
            if (!alwaysSuccess) {
                float maxHealth = mob.getMaxHealth();
                if (maxHealth >= Config.HEALTH_LIMIT.get()) {
                    spawnParticles(mob, false);
                    return InteractionResult.FAIL;
                }
            }
            // 生命值条件
            if (!alwaysSuccess) {
                float currentHealth = mob.getHealth();
                float maxHealth = mob.getMaxHealth();
                boolean healthConditionMet = (currentHealth <= Config.REQUIRED_HEALTH.get()) ||
                        ((currentHealth / maxHealth) * 100.0 <= Config.HEALTH_PERCENT_THRESHOLD.get());
                if (!healthConditionMet) {
                    spawnParticles(mob, false);
                    return InteractionResult.FAIL;
                }
            }
            // 黑名单／已有主人
            if (Config.BLACKLISTED_MOBS.get().contains(EntityType.getKey(mob.getType()).toString()) || hasOwnerOrTameTag(mob)) {
                spawnParticles(mob, false);
                return InteractionResult.FAIL;
            }
            // 高生命值同类限制
            if (MobControlledData.hasPlayerControlledSameHighHealthMob(player.getUUID(), mob)) {
                spawnParticles(mob, false);
                return InteractionResult.FAIL;
            }

            float controlChance = alwaysSuccess ? 1.0f : calculateControlChance(mob);
            if (level.random.nextFloat() <= controlChance) {
                mob.setTarget(null);
                controlMob(player, mob);
                MobControlUtil.showMessageToPlayer(player, mob.getDisplayName(), "mob_controller.mode.follow", new Object[]{}, ChatFormatting.GOLD);
                spawnParticles(mob, true);
                return InteractionResult.SUCCESS;
            } else {
                spawnParticles(mob, false);
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

    // 辅助方法
    private boolean hasOwnerOrTameTag(Mob mob) {
        if (mob instanceof TamableAnimal tamable && tamable.isTame()) return true;
        CompoundTag nbt = mob.saveWithoutId(new CompoundTag());
        if (nbt.contains("Owner") || nbt.contains("OwnerUUID")) return true;
        return nbt.contains("Tame") && nbt.getBoolean("Tame");
    }

    protected float calculateControlChance(Mob mob) {
        if (mob instanceof TamableAnimal tamable && tamable.isTame()) return 0.0f;
        float maxHealth = mob.getMaxHealth();
        if (maxHealth <= 50) return 1.0f;
        float extraHealth = maxHealth - 50;
        int segments = (int) (extraHealth / 50);
        float reduction = segments * 0.2f;
        return Math.max(1.0f - reduction, 0.2f);
    }

    protected void controlMob(Player player, Mob mob) {
        if (mob instanceof Raider raider) {
            Raid raid = raider.getCurrentRaid();
            if (raid != null) raid.removeFromRaid(raider, true);
        }
        MobControlledData.addControlledMob(player.getUUID(), mob);
        if (!mob.level().isClientSide) {
            for (Entity entity : mob.level().getEntitiesOfClass(Entity.class, mob.getBoundingBox().inflate(32.0))) {
                if (entity instanceof Mob oldMob && MobControlledData.isControlledEntity(oldMob)) {
                    if (oldMob.getTarget() != null && oldMob.getTarget().is(mob)) oldMob.setTarget(null);
                    if (mob.getTarget() != null && mob.getTarget().is(oldMob)) mob.setTarget(null);
                }
            }
        }
    }

    protected void spawnParticles(Mob mob, boolean success) {
        if (mob.level().isClientSide) return;
        ServerLevel serverLevel = (ServerLevel) mob.level();
        if (success) {
            serverLevel.sendParticles(ParticleTypes.HEART, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(),
                    7, 0.5, 0.5, 0.5, 0.1);
        } else {
            serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(),
                    7, 0.5, 0.5, 0.5, 0.1);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("mob_controller.tooltip.mob_controller").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("mob_controller.tooltip.mob_controller.desc1").withStyle(ChatFormatting.GOLD));
    }
}