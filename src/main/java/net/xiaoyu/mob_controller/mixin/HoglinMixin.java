package net.xiaoyu.mob_controller.mixin;

import net.xiaoyu.mob_controller.util.MobControlledData;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(Hoglin.class)
public class HoglinMixin {

    // 被控制的疣猪兽不攻击主人
    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void onDoHurtTarget(Entity target, CallbackInfoReturnable<Boolean> cir) {
        Hoglin hoglin = (Hoglin) (Object) this;
        
        if (MobControlledData.isControlledMob(hoglin)) {
            if (target instanceof Player) {
                UUID controllerUUID = MobControlledData.getControllerUUID(hoglin);
                if (controllerUUID != null && target.getUUID().equals(controllerUUID)) {
                    cir.cancel();
                }
            }
        }
    }
}