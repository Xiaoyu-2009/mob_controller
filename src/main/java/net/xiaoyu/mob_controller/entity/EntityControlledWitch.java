package net.xiaoyu.mob_controller.entity;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableWitchTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestHealableRaiderTargetGoal;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xiaoyu.mob_controller.capability.MobControlCapability;
import net.xiaoyu.mob_controller.capability.MobControlCapabilityProvider;
import net.xiaoyu.mob_controller.entity.ai.goal.GoalNearestHealableTarget;
import net.xiaoyu.mob_controller.mixin.AccessorWitch;
import net.xiaoyu.mob_controller.registry.ModEffects;

import javax.annotation.Nullable;
import java.util.UUID;
/**
 * 受控女巫实体。
 *
 * <p>替代原版女巫，支持可控实体队友治疗与主人阵营目标判定。</p>
 */

public class EntityControlledWitch extends Witch implements IControllableEntity {
    /** 可治疗目标选择 AI。 */
    @Nullable
    protected GoalNearestHealableTarget<LivingEntity> goalNearestHealableTarget;

    /**
     * 构造受控女巫。
     */
    public EntityControlledWitch(EntityType<? extends Witch> entityType, Level level) {
        super(entityType, level);
        this.setCanJoinRaid(false);
    }

    /** 注册行为与目标 AI。 */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0D, 60, 10.0F));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(4, new PatrollingMonster.LongDistancePatrolGoal<>(this, 0.7D, 0.595D));

        this.goalNearestHealableTarget = new GoalNearestHealableTarget<>(this, LivingEntity.class, true,
                livingEntity -> this.isSameTeam(livingEntity) && livingEntity.getHealth() < livingEntity.getMaxHealth());
        NearestAttackableWitchTargetGoal<LivingEntity> playerNearestAttackableWitchTargetGoal = new NearestAttackableWitchTargetGoal<>(this, LivingEntity.class, 10, true, false,
                entity -> false);

//        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, this.goalNearestHealableTarget);
        this.targetSelector.addGoal(3, playerNearestAttackableWitchTargetGoal);

        if (this instanceof AccessorWitch accessorWitch) {
            accessorWitch.mob_controller$setHealRaidersGoal(new NearestHealableRaiderTargetGoal<>(this, Raider.class,
                    true, living -> false));
            accessorWitch.mob_controller$setAttackPlayersGoal(playerNearestAttackableWitchTargetGoal);
        }
    }

    /** 每刻更新并递减治疗目标冷却。 */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.goalNearestHealableTarget != null) {
            this.goalNearestHealableTarget.decrementCooldown();
        }
    }

    /**
     * 执行投掷药水攻击/治疗逻辑。
     */
    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (!this.isDrinkingPotion()) {
            Vec3 vec3 = target.getDeltaMovement();
            double d0 = target.getX() + vec3.x - this.getX();
            double d1 = target.getEyeY() - (double) 1.1F - this.getY();
            double d2 = target.getZ() + vec3.z - this.getZ();
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            Potion potion = Potions.HARMING;
            if (target.isInvertedHealAndHarm()) {
                potion = Potions.HEALING;
            }
            if (this.isSameTeam(target)) {
                if (target.getHealth() <= 4.0F || target.hasEffect(MobEffects.REGENERATION)) {
                    potion = ModEffects.SPECIAL_HEALING.get();
                } else {
                    potion = Potions.REGENERATION;
                }
                boolean isOnFire = target.isOnFire() || target.getLastDamageSource() != null && target.getLastDamageSource().is(DamageTypeTags.IS_FIRE);
                if (isOnFire && !target.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                    potion = Potions.LONG_FIRE_RESISTANCE;
                }
                this.setTarget(null);
            } else if (d3 >= 8.0D && !target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                potion = Potions.SLOWNESS;
            } else if (target.getHealth() >= 8.0F && !target.hasEffect(MobEffects.POISON)) {
                potion = Potions.POISON;
            } else if (d3 <= 3.0D && !target.hasEffect(MobEffects.WEAKNESS) && this.random.nextFloat() < 0.25F) {
                potion = Potions.WEAKNESS;
            }

            ThrownPotion thrownpotion = new ThrownPotion(this.level(), this);
            thrownpotion.setItem(PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), potion));
            thrownpotion.setXRot(thrownpotion.getXRot() + 20.0F);
            thrownpotion.shoot(d0, d1 + d3 * 0.2D, d2, 0.75F, 8.0F);
            if (!this.isSilent()) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITCH_THROW, this.getSoundSource(), 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
            }

            this.level().addFreshEntity(thrownpotion);
        }
    }

    /** 在和平模式下不自动消失。 */
    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    /** 女巫可将同阵营单位视作可见目标（用于治疗）。 */
    @Override
    public boolean canSeeAsTarget(LivingEntity living) {
        return IControllableEntity.super.canSeeAsTarget(living) || IControllableEntity.super.isSameTeam(living);
    }

    /** 获取主人 UUID。 */
    @Nullable
    @Override
    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    public UUID getOwnerUUID() {
        return this.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY).resolve().map(MobControlCapability::getControllerUUID).orElse(null);
    }

    /** 设置主人 UUID。 */
    @Override
    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    public void setOwnerUUID(@Nullable UUID uuid) {
        this.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY).ifPresent(cap -> {
            if (uuid != null) {
                cap.setControllerUUID(uuid);
            }
        });
    }

    /** 判断是否受控。 */
    @Override
    public boolean isControlled() {
        return this.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY).resolve().map(MobControlCapability::isControlled).orElse(false);
    }
}
