package net.xiaoyu.mob_controller.mixin;

import net.xiaoyu.mob_controller.util.MobControlledData;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherBoss.class)
public class WitherBossMixin {
    
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void onCustomServerAiStep(CallbackInfo ci) {
        WitherBoss wither = (WitherBoss) (Object) this;
        
        // 被控制的凋零[中立]但不完全...
        if (MobControlledData.isControlledMob(wither)) {
            wither.setAlternativeTarget(0, 0);
            wither.setAlternativeTarget(1, 0);
            wither.setAlternativeTarget(2, 0);
        }
    }
}