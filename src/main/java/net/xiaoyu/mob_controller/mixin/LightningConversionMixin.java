package net.xiaoyu.mob_controller.mixin;

import net.xiaoyu.mob_controller.util.MobControlledData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/**
 * 闪电转化行为注入。
 *
 * <p>阻止受控村民与猪在雷击时发生形态转化。</p>
 */
@Mixin({Villager.class, Pig.class})
public class LightningConversionMixin {

    /**
     * 注入 {@code thunderHit} 头部：受控实体取消雷击转化。
     */
    @Inject(method = "thunderHit", at = @At("HEAD"), cancellable = true)
    private void preventLightningConversion(ServerLevel p_29473_, LightningBolt p_29474_, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // 被控制的村民/猪取消闪电转化
        if (MobControlledData.isControlledEntity(entity)) {
            ci.cancel();
        }
    }
}
