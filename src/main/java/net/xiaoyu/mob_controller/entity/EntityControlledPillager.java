package net.xiaoyu.mob_controller.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.xiaoyu.mob_controller.capability.MobControlCapability;
import net.xiaoyu.mob_controller.capability.MobControlCapabilityProvider;
import net.xiaoyu.mob_controller.entity.ai.goal.GoalOwnerHurtByTarget;
import net.xiaoyu.mob_controller.entity.ai.goal.GoalOwnerHurtTarget;

import javax.annotation.Nullable;
import java.util.UUID;

public class EntityControlledPillager extends Pillager implements IControllableEntity {
    public EntityControlledPillager(EntityType<? extends Pillager> entityType, Level level) {
        super(entityType, level);
        this.setCanJoinRaid(false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new RangedCrossbowAttackGoal<>(this, 1.0D, 8.0F));
        this.goalSelector.addGoal(4, new PatrollingMonster.LongDistancePatrolGoal<>(this, 0.7D, 0.595D));
        this.goalSelector.addGoal(8, new RandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 15.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 15.0F));
        this.targetSelector.addGoal(1, new GoalOwnerHurtByTarget<>(this));
        this.targetSelector.addGoal(2, new GoalOwnerHurtTarget<>(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    public void performCrossbowAttack(LivingEntity user, float velocity) {
        InteractionHand interactionhand = ProjectileUtil.getWeaponHoldingHand(user, item -> item instanceof CrossbowItem);
        ItemStack itemstack = user.getItemInHand(interactionhand);
        int damageValue = 0;
        if (user.isHolding(is -> is.getItem() instanceof CrossbowItem)) {
            damageValue = itemstack.getDamageValue();
        }
        super.performCrossbowAttack(user, velocity);
        itemstack.setDamageValue(damageValue);
    }

    @Nullable
    @Override
    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    public UUID getOwnerUUID() {
        return this.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY).resolve().map(MobControlCapability::getControllerUUID).orElse(null);
    }

    @Override
    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    public void setOwnerUUID(@Nullable UUID uuid) {
        this.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY).ifPresent(cap -> cap.setControllerUUID(uuid));
    }

    @Override
    public boolean isControlled() {
        return this.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY).resolve().map(MobControlCapability::isControlled).orElse(false);
    }
}
