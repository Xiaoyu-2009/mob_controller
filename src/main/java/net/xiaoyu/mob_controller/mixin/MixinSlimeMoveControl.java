package net.xiaoyu.mob_controller.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Mob;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 史莱姆移动控制注入。
 *
 * <p>受控且处于停留模式时，强制速度为 0 并延长跳跃间隔。</p>
 */
@Mixin(targets = "net.minecraft.world.entity.monster.Slime$SlimeMoveControl")
public abstract class MixinSlimeMoveControl {
    @Shadow
    private int jumpDelay;

    /**
     * 包装 {@code tick} 内速度设置：停留模式下冻结移动。
     */
    @WrapOperation(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;setSpeed(F)V"))
    private void wrapTickSetSpeed(Mob instance, float speed, Operation<Void> original) {
        if (MobControlledData.isControlledEntity(instance) && MobControlledData.getControlMode(instance) == MobControlledData.ControlMode.STAY) {
            instance.setSpeed(0);
            this.jumpDelay = 10;
        } else {
            original.call(instance, speed);
        }
    }
}
