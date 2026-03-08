package net.xiaoyu.mob_controller.entity.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.xiaoyu.mob_controller.entity.IControllableEntity;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * @see OwnerHurtByTargetGoal
 */
public class GoalOwnerHurtByTarget<T extends Mob & IControllableEntity> extends TargetGoal {
    private final T controllableEntity;
    @Nullable
    private LivingEntity ownerLastHurtBy;
    private int timestamp;

    public GoalOwnerHurtByTarget(T controllableEntity) {
        super(controllableEntity, false);
        this.controllableEntity = controllableEntity;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (this.controllableEntity.isControlled()) {
            LivingEntity livingentity = this.controllableEntity.getOwner();
            if (livingentity == null) {
                return false;
            } else {
                this.ownerLastHurtBy = livingentity.getLastHurtByMob();
                int i = livingentity.getLastHurtByMobTimestamp();
                if (this.ownerLastHurtBy != null) {
                    return i != this.timestamp && this.canAttack(this.ownerLastHurtBy, TargetingConditions.DEFAULT)
                            && this.controllableEntity.wantsToAttack(this.ownerLastHurtBy);
                }
            }
        }
        return false;
    }

    @Override
    public void start() {
        this.mob.setTarget(this.ownerLastHurtBy);
        LivingEntity livingentity = this.controllableEntity.getOwner();
        if (livingentity != null) {
            this.timestamp = livingentity.getLastHurtByMobTimestamp();
        }

        super.start();
    }
}
