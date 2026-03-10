package net.xiaoyu.mob_controller.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Mob;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.monster.Slime$SlimeMoveControl")
public class MixinSlimeMoveControl {
    @Shadow
    private int jumpDelay;

    @WrapOperation(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;setSpeed(F)V"))
    private void wrapTickSetSpeed(Mob instance, float speed, Operation<Void> original) {
        if (MobControlledData.isControlledMob(instance) && MobControlledData.getControlMode(instance) == MobControlledData.ControlMode.STAY) {
            instance.setSpeed(0);
            this.jumpDelay = 10;
        } else {
            original.call(instance, speed);
        }
    }
}
