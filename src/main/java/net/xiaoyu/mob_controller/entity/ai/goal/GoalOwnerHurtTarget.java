package net.xiaoyu.mob_controller.entity.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.xiaoyu.mob_controller.entity.IControllableEntity;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * @see OwnerHurtTargetGoal
 */
public class GoalOwnerHurtTarget<T extends Mob & IControllableEntity> extends TargetGoal {
    private final T controllableEntity;
    @Nullable
    private LivingEntity ownerLastHurt;
    private int timestamp;

    public GoalOwnerHurtTarget(T controllableEntity) {
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
                this.ownerLastHurt = livingentity.getLastHurtMob();
                int i = livingentity.getLastHurtMobTimestamp();
                if (this.ownerLastHurt != null) {
                    return i != this.timestamp && this.canAttack(this.ownerLastHurt, TargetingConditions.DEFAULT)
                            && this.controllableEntity.wantsToAttack(this.ownerLastHurt);
                }
            }
        }
        return false;
    }

    @Override
    public void start() {
        this.mob.setTarget(this.ownerLastHurt);
        LivingEntity livingentity = this.controllableEntity.getOwner();
        if (livingentity != null) {
            this.timestamp = livingentity.getLastHurtMobTimestamp();
        }

        super.start();
    }
}
