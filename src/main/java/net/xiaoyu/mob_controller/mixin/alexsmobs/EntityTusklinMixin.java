package net.xiaoyu.mob_controller.mixin.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityTusklin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.xiaoyu.mob_controller.mixin.AccessorLivingEntity;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityTusklin.class)
public abstract class EntityTusklinMixin {

    @Shadow(remap = false) private int ridingTime;
    @Shadow(remap = false) private int conversionTime;

    @Unique
    private LivingEntity mob_controller$originalTarget;

    @Unique
    private boolean isControlledRider(EntityTusklin self, Player rider) {
        return MobControlledData.isControlledEntity(self) &&
                rider != null &&
                rider.getUUID().equals(MobControlledData.getControllerUUID(self));
    }

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void onTick(CallbackInfo ci) {
        EntityTusklin self = (EntityTusklin) (Object) this;
        if (MobControlledData.isControlledEntity(self)) {
            if (self.isInNether()) conversionTime = 0;
            Player rider = self.getControllingPassenger() instanceof Player ? (Player) self.getControllingPassenger() : null;
            if (isControlledRider(self, rider)) {
                // 控制者骑乘：重置 ridingTime，永不甩下
                if (self.isVehicle()) ridingTime = 0;
            } else if (rider != null) {
                // 非控制者骑乘：立即触发甩人（设置 ridingTime 超过最大允许值）
                ridingTime = 10000;
            }
        }
    }

    @Inject(method = "getRiddenInput", at = @At("HEAD"), cancellable = true, require = 0)
    private void onGetRiddenInput(Player player, Vec3 deltaIn, CallbackInfoReturnable<Vec3> cir) {
        EntityTusklin self = (EntityTusklin) (Object) this;
        if (isControlledRider(self, player)) {
            float strafe = player.xxa * 0.5f;
            float forward = player.zza;
            if (forward <= 0) forward *= 0.25f;
            if (self.isInWater() && ((AccessorLivingEntity) player).mob_controller$getJumping()) {
                cir.setReturnValue(new Vec3(strafe, 1.0, forward));
            } else {
                cir.setReturnValue(new Vec3(strafe, 0, forward));
            }
        }
    }

    @Inject(method = "tickRidden", at = @At("HEAD"))
    private void captureTarget(Player player, Vec3 deltaIn, CallbackInfo ci) {
        EntityTusklin self = (EntityTusklin) (Object) this;
        if (MobControlledData.isControlledEntity(self)) {
            mob_controller$originalTarget = self.getTarget();
        }
    }

    @Inject(method = "tickRidden", at = @At("RETURN"))
    private void restoreTarget(Player player, Vec3 deltaIn, CallbackInfo ci) {
        EntityTusklin self = (EntityTusklin) (Object) this;
        if (MobControlledData.isControlledEntity(self) && mob_controller$originalTarget != null && self.getTarget() == null) {
            self.setTarget(mob_controller$originalTarget);
        }
        mob_controller$originalTarget = null;
    }

    @Inject(method = "tickRidden", at = @At("RETURN"), require = 0)
    private void onTickRidden(Player player, Vec3 deltaIn, CallbackInfo ci) {
        EntityTusklin self = (EntityTusklin) (Object) this;
        if (!isControlledRider(self, player)) return;

        self.setYRot(player.getYRot());
        self.yRotO = self.yBodyRot = self.yHeadRot = self.getYRot();
        AccessorLivingEntity accessor = (AccessorLivingEntity) player;
        if (accessor.mob_controller$getJumping() && self.onGround()) {
            double jumpHeight = 0.42;
            self.setDeltaMovement(self.getDeltaMovement().x, jumpHeight, self.getDeltaMovement().z);
            self.hasImpulse = true;
            ForgeHooks.onLivingJump(self);
        }
    }

    @Inject(method = "getRiddenSpeed", at = @At("HEAD"), cancellable = true, require = 0)
    private void onGetRiddenSpeed(Player rider, CallbackInfoReturnable<Float> cir) {
        EntityTusklin self = (EntityTusklin) (Object) this;
        if (isControlledRider(self, rider)) {
            cir.setReturnValue((float) self.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getValue());
        }
    }

    @Inject(method = "getControllingPassenger", at = @At("HEAD"), cancellable = true, require = 0)
    private void onGetControllingPassenger(CallbackInfoReturnable<LivingEntity> cir) {
        EntityTusklin self = (EntityTusklin) (Object) this;
        if (MobControlledData.isControlledEntity(self) && self.isSaddled()) {
            for (Entity p : self.getPassengers()) {
                if (p instanceof Player && p.getUUID().equals(MobControlledData.getControllerUUID(self))) {
                    cir.setReturnValue((LivingEntity) p);
                    return;
                }
            }
        }
        // 若无控制者，让原方法继续
    }
}