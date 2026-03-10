package net.xiaoyu.mob_controller.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(PathNavigation.class)
public abstract class MixinPathNavigation {
    @Shadow
    @Final
    protected Mob mob;

    @Inject(method = "createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;", at = @At("HEAD"), cancellable = true)
    private void injectCreatePath(Set<BlockPos> targets, int regionOffset, boolean offsetUpward,
                                  int accuracy, float followRange, CallbackInfoReturnable<Path> cir) {
        if (MobControlledData.isControlledMob(mob) && MobControlledData.getControlMode(mob) == MobControlledData.ControlMode.STAY) {
            cir.setReturnValue(null);
        }
    }
}
