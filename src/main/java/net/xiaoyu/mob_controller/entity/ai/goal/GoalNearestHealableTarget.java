package net.xiaoyu.mob_controller.entity.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestHealableRaiderTargetGoal;
import net.minecraft.world.entity.raid.Raider;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * @see NearestHealableRaiderTargetGoal
 */
public class GoalNearestHealableTarget<T extends LivingEntity> extends NearestHealableRaiderTargetGoal<T> {
    public GoalNearestHealableTarget(Raider mob, Class<T> targetType, boolean mustSee, @Nullable Predicate<LivingEntity> targetPredicate) {
        super(mob, targetType, mustSee, targetPredicate);
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
}
