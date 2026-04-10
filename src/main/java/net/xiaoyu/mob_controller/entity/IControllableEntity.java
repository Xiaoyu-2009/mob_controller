package net.xiaoyu.mob_controller.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.xiaoyu.mob_controller.util.MobControlledData;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * 可控实体接口。
 *
 * <p>为实体提供“主人”语义，并封装默认的敌友判定规则，
 * 供受控灾厄村民等实体复用。</p>
 */
public interface IControllableEntity extends OwnableEntity {
    /**
     * 设置主人 UUID。
     *
     * @param uuid 主人 UUID，可为 {@code null}
     */
    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    void setOwnerUUID(@Nullable UUID uuid);

    /**
     * 判断实体当前是否处于受控状态。
     *
     * @return {@code true} 表示实体已绑定主人
     */
    boolean isControlled();

    /**
     * 判断该实体是否希望攻击指定目标。
     *
     * @param target 候选目标
     * @return {@code true} 表示可攻击
     */
    default boolean wantsToAttack(LivingEntity target) {
        if (target instanceof OwnableEntity ownable && Objects.equals(ownable.getOwnerUUID(), this.getOwnerUUID())) {
            return false;
        }
        if (Objects.equals(target, this.getOwner())) {
            return false;
        }
        return !(target instanceof Mob mob) || !MobControlledData.isControlledEntity(target)
               || !Objects.equals(MobControlledData.getControllerUUID(mob), this.getOwnerUUID());
    }

    /**
     * 判断是否视作同一阵营。
     *
     * @param living 需要判断的生物
     * @return {@code true} 表示同阵营
     */
    default boolean isSameTeam(LivingEntity living) {
        if (living instanceof OwnableEntity ownable && Objects.equals(ownable.getOwnerUUID(), this.getOwnerUUID())) {
            return true;
        }
        if (Objects.equals(living, this.getOwner())) {
            return true;
        }
        return living instanceof Mob mob && MobControlledData.isControlledEntity(living)
               && Objects.equals(MobControlledData.getControllerUUID(mob), this.getOwnerUUID());
    }

    /**
     * 判断该生物是否可作为目标被锁定。
     *
     * @param living 候选目标
     * @return {@code true} 表示可作为目标
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    default boolean canSeeAsTarget(LivingEntity living) {
        return wantsToAttack(living);
    }
}
