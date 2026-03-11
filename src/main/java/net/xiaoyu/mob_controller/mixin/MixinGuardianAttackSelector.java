package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.monster.Guardian$GuardianAttackSelector")
public class MixinGuardianAttackSelector {
    @Shadow
    @Final
    private Guardian guardian;

    @Inject(method = "test(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("RETURN"), cancellable = true)
    private void injectTest(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (MobControlledData.isControlledEntity(guardian) && !MobControlUtil.canControlledMobAttackTarget(guardian, entity)) {
            cir.setReturnValue(false);
        }
    }
}
