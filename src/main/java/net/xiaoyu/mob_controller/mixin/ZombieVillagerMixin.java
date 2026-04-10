package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.monster.ZombieVillager;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/**
 * 僵尸村民转化行为注入。
 *
 * <p>阻止受控僵尸村民完成治愈并转化回村民。</p>
 */
@Mixin(ZombieVillager.class)
public class ZombieVillagerMixin {

    /**
     * 注入 {@code finishConversion} 头部：受控僵尸村民取消转化。
     */
    @Inject(
            method = "finishConversion(Lnet/minecraft/server/level/ServerLevel;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void preventConversion(CallbackInfo ci) {
        ZombieVillager zombieVillager = (ZombieVillager) (Object) this;

        // 被控制的僵尸村民取消转换村民
        if (MobControlledData.isControlledEntity(zombieVillager)) {
            ci.cancel();
        }
    }
}
