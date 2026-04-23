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

/**
 * 生物寻路行为注入。
 *
 * <p>在受控生物处于“停留”模式时禁止创建新路径。</p>
 */
@Mixin(PathNavigation.class)
public abstract class MixinPathNavigation {
    @Shadow
    @Final
    protected Mob mob;

    /**
     * 注入 {@code createPath} 头部：停留模式下直接返回空路径。
     */
    @Inject(method = "createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;", at = @At("HEAD"), cancellable = true)
    private void injectCreatePath(
        Set<BlockPos> targets, int regionOffset, boolean offsetUpward,
        int accuracy, float followRange, CallbackInfoReturnable<Path> cir
    ) {
        if (MobControlledData.isControlledEntity(mob) && MobControlledData.getControlMode(mob) == MobControlledData.ControlMode.STAY) {
            cir.setReturnValue(null);
        }
    }
}
