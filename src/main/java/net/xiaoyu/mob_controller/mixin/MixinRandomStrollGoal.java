package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RandomStrollGoal.class)
public abstract class MixinRandomStrollGoal {
    @Shadow
    @Final
    protected PathfinderMob mob;
    
    @Inject(method = "canUse()Z", at = @At("HEAD"), cancellable = true)
    private void injectCanUse(CallbackInfoReturnable<Boolean> cir) {
        if (MobControlledData.isControlledEntity(this.mob) && (MobControlledData.getControlMode(this.mob) == MobControlledData.ControlMode.STAY)) {
            cir.setReturnValue(false);
        }
    }
}
