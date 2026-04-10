package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.monster.Slime;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 史莱姆随机转向目标注入。
 *
 * <p>受控且处于跟随模式时，禁止该随机转向目标启动。</p>
 */
@Mixin(targets = "net.minecraft.world.entity.monster.Slime$SlimeRandomDirectionGoal")
public abstract class MixinSlimeRandomDirectionGoal {
    @Shadow
    @Final
    private Slime slime;

    /**
     * 注入 {@code canUse} 返回点：跟随模式下返回 false。
     */
    @Inject(method = "canUse()Z", at = @At("RETURN"), cancellable = true)
    private void injectCanUse(CallbackInfoReturnable<Boolean> cir) {
        if (MobControlledData.getControlMode(this.slime) == MobControlledData.ControlMode.FOLLOW) {
            cir.setReturnValue(false);
        }
    }
}
