package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.monster.Slime;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.monster.Slime$SlimeRandomDirectionGoal")
public abstract class MixinSlimeRandomDirectionGoal {
    @Shadow
    @Final
    private Slime slime;

    @Inject(method = "canUse()Z", at = @At("RETURN"), cancellable = true)
    private void injectCanUse(CallbackInfoReturnable<Boolean> cir) {
        if (MobControlledData.getControlMode(this.slime) == MobControlledData.ControlMode.FOLLOW) {
            cir.setReturnValue(false);
        }
    }
}
