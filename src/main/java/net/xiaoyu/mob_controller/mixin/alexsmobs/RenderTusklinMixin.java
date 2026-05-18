package net.xiaoyu.mob_controller.mixin.alexsmobs;

import com.github.alexthe666.alexsmobs.client.render.RenderTusklin;
import com.github.alexthe666.alexsmobs.entity.EntityTusklin;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTusklin.class)
public class RenderTusklinMixin {

    @Inject(method = "isShaking", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onIsShaking(EntityTusklin entity, CallbackInfoReturnable<Boolean> cir) {
        if (MobControlledData.isControlledEntity(entity)) {
            cir.setReturnValue(false);
        }
    }
}