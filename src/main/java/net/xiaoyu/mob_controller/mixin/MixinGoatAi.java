package net.xiaoyu.mob_controller.mixin;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.goat.GoatAi;
import net.minecraft.world.entity.schedule.Activity;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GoatAi.class)
public class MixinGoatAi {

    @Inject(method = "updateActivity(Lnet/minecraft/world/entity/animal/goat/Goat;)V",
            at = @At("HEAD"), cancellable = true)
    private static void onUpdateActivity(Goat goat, CallbackInfo ci) {
        if (MobControlledData.isControlledEntity(goat) &&
                MobControlledData.getControlMode(goat) == MobControlledData.ControlMode.STAY) {
            // 强制将当前活动设置为 IDLE
            goat.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.IDLE));
            ci.cancel();
        }
    }
}