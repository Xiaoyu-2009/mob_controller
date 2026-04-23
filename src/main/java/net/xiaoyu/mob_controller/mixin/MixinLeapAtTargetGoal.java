package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LeapAtTargetGoal.class)
public abstract class MixinLeapAtTargetGoal {
    @Shadow
    @Final
    private Mob mob;
    
    @Inject(method = "canUse()Z", at = @At("HEAD"), cancellable = true)
    private void injectCanUse(CallbackInfoReturnable<Boolean> cir) {
        if (MobControlledData.isControlledEntity(this.mob) && (MobControlledData.getControlMode(this.mob) == MobControlledData.ControlMode.STAY)) {
            cir.setReturnValue(false);
        }
    }
}
