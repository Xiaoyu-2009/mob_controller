package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.monster.Guardian;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.monster.Guardian$GuardianAttackGoal")
public class MixinGuardianAttackGoal {
    @Shadow
    @Final
    private Guardian guardian;

    @Inject(method = "canContinueToUse()Z", at = @At("RETURN"), cancellable = true)
    private void injectCanContinueToUse(CallbackInfoReturnable<Boolean> cir) {
        if (MobControlledData.isControlledEntity(guardian) && !MobControlUtil.isEnemy(guardian, guardian.getTarget())) {
            cir.setReturnValue(false);
        }
    }
}
