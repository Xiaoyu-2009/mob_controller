package net.xiaoyu.mob_controller.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.xiaoyu.mob_controller.util.MobControlledData;

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
        if (Objects.equals(target, this.getOwner())) {
            return false;
        }
        return !(target instanceof Mob mob) || !MobControlledData.isControlledEntity(target)
                || !MobControlledData.getControllerUUID(mob).equals(this.getOwnerUUID());
    }

    default boolean isSameTeam(LivingEntity living) {
        if (living instanceof OwnableEntity ownable && Objects.equals(ownable.getOwnerUUID(), this.getOwnerUUID())) {
            return true;
        }
        if (Objects.equals(living, this.getOwner())) {
            return true;
        }
        return living instanceof Mob mob && MobControlledData.isControlledEntity(living)
                && MobControlledData.getControllerUUID(mob).equals(this.getOwnerUUID());
    }

    default boolean canSeeAsTarget(LivingEntity living) {
        return wantsToAttack(living);
    }
}
