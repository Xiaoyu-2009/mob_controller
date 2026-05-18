package net.xiaoyu.mob_controller.mixin.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityCachalotWhale;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityCachalotWhale.class)
public class MixinCachalotWhaleSleep {

    /**
     * 复制原版 isSleepTime() 的逻辑（避免调用私有方法）
     */
    private boolean originalIsSleepTime(EntityCachalotWhale whale) {
        long time = whale.level().getDayTime();
        return time > 18000 && time < 22812 && whale.isInWaterOrBubble();
    }

    @Redirect(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/github/alexthe666/alexsmobs/entity/EntityCachalotWhale;isSleepTime()Z",
                    remap = false
            ),
            require = 0  // 关键：即使目标不存在也不崩溃
    )
    private boolean redirectIsSleepTime(EntityCachalotWhale whale) {
        // 1. 有任何乘客 → 禁止睡眠
        if (!whale.getPassengers().isEmpty()) {
            return false;
        }
        // 2. 受控且跟随模式 → 禁止睡眠
        if (MobControlledData.isControlledEntity(whale)
                && MobControlledData.getControlMode(whale) == MobControlledData.ControlMode.FOLLOW) {
            return false;
        }
        // 3. 否则使用原版睡眠判定逻辑
        return originalIsSleepTime(whale);
    }
}