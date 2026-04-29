package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Vex;
import net.xiaoyu.mob_controller.capability.MobControlCapabilityProvider;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(Vex.class)
public abstract class VexMixin {

    @Unique
    private int mob_controller$lastSyncedTargetId = -1;

    @Inject(method = "setOwner", at = @At("TAIL"))
    private void onSetOwner(Mob owner, CallbackInfo ci) {
        Vex vex = (Vex) (Object) this;
        if (owner instanceof Evoker evoker && MobControlledData.isControlledEntity(evoker)) {
            UUID controllerUUID = MobControlledData.getControllerUUID(evoker);
            if (controllerUUID != null && !MobControlledData.isControlledEntity(vex)) {
                // 加入控制，不持久化，标记为召唤物
                MobControlledData.addControlledMob(controllerUUID, vex, false);
                vex.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY)
                        .ifPresent(cap -> cap.setSummoned(true));
                // 立即同步一次目标
                syncTargetWithOwner(vex, evoker);
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        Vex vex = (Vex) (Object) this;
        if (!MobControlledData.isControlledEntity(vex)) return;
        if (!MobControlledData.isSummoned(vex)) return; // 仅召唤物恼鬼自动同步目标

        Mob owner = vex.getOwner();
        if (owner == null) return;
        // 每 20 ticks 同步一次主人目标
        if (vex.tickCount % 20 == 0) {
            syncTargetWithOwner(vex, owner);
        }
    }

    @Unique
    private void syncTargetWithOwner(Vex vex, Mob owner) {
        LivingEntity ownerTarget = owner.getTarget();
        int targetId = ownerTarget != null ? ownerTarget.getId() : -1;
        if (targetId == mob_controller$lastSyncedTargetId) return;
        mob_controller$lastSyncedTargetId = targetId;

        LivingEntity currentTarget = vex.getTarget();
        if (ownerTarget == null || !ownerTarget.isAlive()) {
            if (currentTarget != null) {
                vex.setTarget(null);
            }
            return;
        }

        if (currentTarget != ownerTarget) {
            MobControlledData.markSystemAttack(vex);
            vex.setTarget(ownerTarget);
        }
    }
}