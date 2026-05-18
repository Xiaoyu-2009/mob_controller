package net.xiaoyu.mob_controller.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.server.entity.living.VesperEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VesperEntity.class)
public class MixinVesper {

    /**
     * 在 Vesper 的 tick 方法末尾注入，处理骑乘时的飞行状态。
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        VesperEntity vesper = (VesperEntity) (Object) this;

        // 如果有骑乘的乘客
        if (vesper.isVehicle()) {
            var firstPassenger = vesper.getFirstPassenger();
            // 乘客必须是玩家
            if (firstPassenger instanceof Player) {
                // 如果不在地面上，强制开启飞行
                if (!vesper.onGround()) {
                    if (!vesper.isFlying()) {
                        vesper.setFlying(true);
                    }
                } else {
                    // 落回地面时，如果玩家还骑着，可以关闭飞行（可选）
                    if (vesper.isFlying()) {
                        vesper.setFlying(false);
                    }
                }
            }
        } else {
            // 没有乘客时，不做干预，让 Vesper 的原逻辑自行决定飞行/悬挂
        }
    }
}