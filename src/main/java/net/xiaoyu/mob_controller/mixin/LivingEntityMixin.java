package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.xiaoyu.mob_controller.capability.WaxedCapability;
import net.xiaoyu.mob_controller.capability.WaxedCapabilityProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    // 访问器：调用原始的 isSensitiveToWater 方法（避免递归）
    @Shadow
    public abstract boolean isSensitiveToWater();

    /**
     * 重定向 aiStep 中对 isSensitiveToWater 的调用。
     * 如果生物已打蜡，返回 false（不受伤）；否则返回原始值。
     */
    @Redirect(
            method = "aiStep",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSensitiveToWater()Z")
    )
    private boolean redirectIsSensitiveToWater(LivingEntity self) {
        // 注意：这里的 self 就是当前对象，通过 capability 检查打蜡状态
        return self.getCapability(WaxedCapabilityProvider.WAXED_CAPABILITY)
                .map(WaxedCapability::isWaxed)
                .orElse(false) ? false : self.isSensitiveToWater(); // 调用原始方法（不会递归，因为 method 是 @Shadow 重定向的目标）
    }
}