package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Hoglin.class)
public class HoglinMixin {
    /**
     * 被控制的疣猪兽不攻击主人
     */
    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void onDoHurtTarget(Entity target, CallbackInfoReturnable<Boolean> cir) {
        Hoglin hoglin = (Hoglin) (Object) this;
        if (MobControlledData.isControlledMob(hoglin) && MobControlUtil.canControlledMobAttackTarget(hoglin, target)) {
            cir.cancel();
        }
    }
}