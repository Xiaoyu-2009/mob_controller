package net.xiaoyu.mob_controller.mixin;

import net.xiaoyu.mob_controller.util.MobControlledData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zoglin.class)
public class ZoglinMixin {

    // 被控制的僵尸疣猪兽不攻击主人
    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void onDoHurtTarget(Entity target, CallbackInfoReturnable<Boolean> cir) {
        Zoglin zoglin = (Zoglin) (Object) this;
        
        if (MobControlledData.isControlledMob(zoglin)) {
            if (target instanceof Player) {
                if (target.getUUID().equals(MobControlledData.getControllerUUID(zoglin))) {
                    cir.cancel();
                }
            }
        }
    }
}