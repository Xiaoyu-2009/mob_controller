package net.xiaoyu.mob_controller.mixin;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.frog.FrogAi;
import net.minecraft.world.entity.schedule.Activity;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FrogAi.class)
public class MixinFrogAi {

    @Inject(method = "updateActivity(Lnet/minecraft/world/entity/animal/frog/Frog;)V",
            at = @At("HEAD"), cancellable = true)
    private static void onUpdateActivity(Frog frog, CallbackInfo ci) {
        if (MobControlledData.isControlledEntity(frog) &&
                MobControlledData.getControlMode(frog) == MobControlledData.ControlMode.STAY) {
            // 构建不包含 LONG_JUMP 的活动列表，保留其他活动
            ImmutableList<Activity> activitiesWithoutJump = ImmutableList.of(
                    Activity.TONGUE,
                    Activity.LAY_SPAWN,
                    Activity.SWIM,
                    Activity.IDLE
            );
            frog.getBrain().setActiveActivityToFirstValid(activitiesWithoutJump);
            ci.cancel(); // 阻止原方法执行，避免再次覆盖
        }
    }
}