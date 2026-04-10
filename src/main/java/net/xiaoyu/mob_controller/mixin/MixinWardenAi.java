package net.xiaoyu.mob_controller.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenAi;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 监守者 AI 注入。
 *
 * <p>阻止受控监守者接收基于声音扰动的位置调查指令。</p>
 */
@Mixin(WardenAi.class)
public class MixinWardenAi {
    /**
     * 注入 {@code setDisturbanceLocation} 头部：受控监守者忽略声音扰动。
     */
    @Inject(method = "setDisturbanceLocation", at = @At("HEAD"), cancellable = true)
    private static void ignoreSoundDisturbanceForControlledWarden(Warden warden, BlockPos disturbanceLocation, CallbackInfo ci) {
        if (MobControlledData.isControlledEntity(warden)) {
            ci.cancel();
        }
    }
}

