package net.xiaoyu.mob_controller.entity.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.xiaoyu.mob_controller.entity.IControllableEntity;

import java.util.EnumSet;
import javax.annotation.Nullable;

/**
 * 受控实体“主人攻击目标”AI。
 *
 * <p>当主人攻击某目标时，受控实体会尝试将其设为攻击目标。</p>
 *
 * @param <T> 受控实体类型
 * @see OwnerHurtTargetGoal
 */
public class GoalOwnerHurtTarget<T extends Mob & IControllableEntity> extends TargetGoal {
    private final T controllableEntity;
    @Nullable
    private LivingEntity ownerLastHurt;
    private int timestamp;

    /**
     * 构造目标 AI。
     *
     * @param controllableEntity 受控实体
     */
    public GoalOwnerHurtTarget(T controllableEntity) {
        super(controllableEntity, false);
        this.controllableEntity = controllableEntity;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    /**
     * 判断是否满足激活条件。
     */
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

    /**
     * 启动 AI 并同步时间戳，避免重复触发。
     */
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
