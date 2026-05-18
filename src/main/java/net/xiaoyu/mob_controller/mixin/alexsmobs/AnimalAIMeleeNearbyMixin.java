package net.xiaoyu.mob_controller.mixin.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIMeleeNearby;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AnimalAIMeleeNearby.class)
public abstract class AnimalAIMeleeNearbyMixin {

    /**
     * 包装 canUse() 方法中对 entity.isVehicle() 的调用，
     * 无论实际骑乘状态如何均返回 false，从而移除 !entity.isVehicle() 的限制。
     */
    @WrapOperation(
            method = "canUse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Mob;isVehicle()Z"
            )
    )
    private boolean forceNotVehicle(Mob entity, Operation<Boolean> original) {
        // 强制返回 false，使 !false = true，即条件永远通过
        return false;
    }
}