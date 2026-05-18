package net.xiaoyu.mob_controller.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.SnowGolem;
import net.xiaoyu.mob_controller.capability.ChilledCapability;
import net.xiaoyu.mob_controller.capability.ChilledCapabilityProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SnowGolem.class)
public class SnowGolemMixin {

    /**
     * 重定向 aiStep 中的 hurt 调用，若雪傀儡已冷冻则跳过炎热伤害。
     */
    @Redirect(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/animal/SnowGolem;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            )
    )
    private boolean preventMeltingDamage(SnowGolem golem, DamageSource source, float amount) {
        // 检查是否冷冻
        boolean chilled = golem.getCapability(ChilledCapabilityProvider.CHILLED_CAPABILITY)
                .map(ChilledCapability::isChilled)
                .orElse(false);
        if (chilled) {
            return false; // 不受炎热伤害
        }
        // 否则执行原版伤害
        return golem.hurt(source, amount);
    }
}