package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.animal.Fox;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Fox.class)
public class MixinFox {

    @Inject(method = "tick()V", at = @At("RETURN"))
    private void onTick(CallbackInfo ci) {
        Fox fox = (Fox) (Object) this;

        if (!MobControlledData.isControlledEntity(fox)) {
            return;
        }

        boolean isStay = MobControlledData.getControlMode(fox) == MobControlledData.ControlMode.STAY;

        if (isStay && !fox.isSitting()) {
            fox.setSitting(true);
        } else if (!isStay && fox.isSitting()) {
            fox.setSitting(false);
        }
    }
}