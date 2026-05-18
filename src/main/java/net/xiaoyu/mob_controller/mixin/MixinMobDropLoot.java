package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.config.FeatureConfig;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 阻止受控生物在重生模式开启且禁止掉落时，因原版 dropCustomDeathLoot 而清除装备槽位。
 * 通过直接返回跳过整个方法，装备不会被移除，也不会生成装备掉落物。
 */
@Mixin(Mob.class)
public class MixinMobDropLoot {

    @Inject(method = "dropCustomDeathLoot", at = @At("HEAD"), cancellable = true)
    private void onDropCustomDeathLoot(DamageSource source, int lootingLevel, boolean recentlyHit, CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        // 仅在重生开启且配置阻止掉落时生效
        if (Config.ENABLE_RESPAWN.get() && FeatureConfig.PREVENT_DROPS_ON_RESPAWN.get()) {
            if (MobControlledData.isControlledEntity(self)) {
                // 跳过原装备掉落与清除逻辑，保留装备槽位
                ci.cancel();
            }
        }
    }
}