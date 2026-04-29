package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HurtByTargetGoal.class)
public class MixinHurtByTargetGoal {

    @Inject(method = "alertOther", at = @At("HEAD"), cancellable = true)
    private void onAlertOther(Mob mob, LivingEntity target, CallbackInfo ci) {
        if (MobControlledData.isControlledEntity(mob)) {
            ci.cancel();
        }
    }
}