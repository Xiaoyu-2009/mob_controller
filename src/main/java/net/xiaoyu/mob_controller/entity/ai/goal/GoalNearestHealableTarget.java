package net.xiaoyu.mob_controller.entity.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestHealableRaiderTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import java.util.function.Predicate;

/**
 * @see NearestHealableRaiderTargetGoal
 */
public class GoalNearestHealableTarget<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
    private static final int DEFAULT_COOLDOWN = 200;
    private int cooldown = 0;

    public GoalNearestHealableTarget(Mob mob, Class<T> targetType, boolean mustSee, Predicate<LivingEntity> targetPredicate) {
        super(mob, targetType, mustSee, targetPredicate);
        this.targetConditions = TargetingConditions.forNonCombat().range(this.getFollowDistance()).selector(targetPredicate);
    }

    public int getCooldown() {
        return this.cooldown;
    }

    public void decrementCooldown() {
        --this.cooldown;
    }

    @Override
    public boolean canUse() {
        if (this.getCooldown() <= 0 && this.mob.getRandom().nextBoolean()) {
            this.findTarget();
            return this.target != null;
        } else {
            return false;
        }
    }

    @Override
    public void start() {
        this.cooldown = reducedTickDelay(DEFAULT_COOLDOWN);
        super.start();
    }
}
