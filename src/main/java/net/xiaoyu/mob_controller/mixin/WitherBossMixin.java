package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 凋零行为注入。
 *
 * <p>受控凋零在服务端 AI 步骤中清空副头替代目标，降低误伤。</p>
 */
@Mixin(WitherBoss.class)
public class WitherBossMixin {

    /**
     * 注入 {@code customServerAiStep} 头部：受控状态下清除三个替代目标。
     */
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void onCustomServerAiStep(CallbackInfo ci) {
        WitherBoss wither = (WitherBoss) (Object) this;

        // 被控制的凋零[中立]但不完全...
        if (MobControlledData.isControlledEntity(wither)) {
            wither.setAlternativeTarget(0, 0);
            wither.setAlternativeTarget(1, 0);
            wither.setAlternativeTarget(2, 0);
        }
    }
}
