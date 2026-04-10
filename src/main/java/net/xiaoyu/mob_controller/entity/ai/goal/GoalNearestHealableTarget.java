package net.xiaoyu.mob_controller.entity.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestHealableRaiderTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import java.util.function.Predicate;

/**
 * 受控实体的“最近可治疗目标”选择 AI。
 *
 * <p>基于非战斗条件筛选同阵营低血量单位，并附加冷却以避免过于频繁地重选目标。</p>
 *
 * @param <T> 目标类型
 * @see NearestHealableRaiderTargetGoal
 */
public class GoalNearestHealableTarget<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
    /** 默认冷却（tick）。 */
    private static final int DEFAULT_COOLDOWN = 200;
    private int cooldown = 0;

    /**
     * 构造可治疗目标选择 AI。
     *
     * @param mob             执行者
     * @param targetType      目标类型
     * @param mustSee         是否要求可见
     * @param targetPredicate 目标筛选条件
     */
    public GoalNearestHealableTarget(Mob mob, Class<T> targetType, boolean mustSee, Predicate<LivingEntity> targetPredicate) {
        super(mob, targetType, mustSee, targetPredicate);
        this.targetConditions = TargetingConditions.forNonCombat().range(this.getFollowDistance()).selector(targetPredicate);
    }

    /**
     * 获取剩余冷却。
     *
     * @return 冷却 tick
     */
    public int getCooldown() {
        return this.cooldown;
    }

    /** 每刻递减冷却。 */
    public void decrementCooldown() {
        --this.cooldown;
    }

    /**
     * 冷却完成后尝试寻找目标。
     */
    @Override
    public boolean canUse() {
        if (this.getCooldown() <= 0 && this.mob.getRandom().nextBoolean()) {
            this.findTarget();
            return this.target != null;
        } else {
            return false;
        }
    }

    /**
     * 启动 AI 时重置冷却。
     */
    @Override
    public void start() {
        this.cooldown = reducedTickDelay(DEFAULT_COOLDOWN);
        super.start();
    }
}
