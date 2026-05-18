package net.xiaoyu.mob_controller.mixin.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityTusklin;
import com.github.alexthe666.alexsmobs.entity.ai.TameableAIRide;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TameableAIRide.class)
public abstract class TameableAIRideMixin {

    /**
     * 包装 tameableEntity.setTarget(null) 调用，受控 Tusklin 跳过清空目标。
     */
    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/PathfinderMob;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"
            )
    )
    private void wrapSetTarget(PathfinderMob tameableEntity, LivingEntity target, Operation<Void> original) {
        // 如果是 Tusklin 且受控，则忽略 setTarget(null) 调用
        if (tameableEntity instanceof EntityTusklin && MobControlledData.isControlledEntity(tameableEntity)) {
            return;
        }
        // 否则执行原始调用
        original.call(tameableEntity, target);
    }
}