package net.xiaoyu.mob_controller.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public interface IControllableEntity extends OwnableEntity {
    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    void setOwnerUUID(@Nullable UUID uuid);

    boolean isControlled();

    default boolean wantsToAttack(LivingEntity target) {
        if (target instanceof OwnableEntity ownable && Objects.equals(ownable.getOwnerUUID(), this.getOwnerUUID())) {
            return false;
        }
        return !Objects.equals(target, this.getOwner());
    }

    default boolean isSameTeam(LivingEntity living) {
        if (living instanceof OwnableEntity ownable && Objects.equals(ownable.getOwnerUUID(), this.getOwnerUUID())) {
            return true;
        }
        return Objects.equals(living, this.getOwner());
    }

    default boolean canSeeAsTarget(LivingEntity living) {
        return wantsToAttack(living);
    }
}
