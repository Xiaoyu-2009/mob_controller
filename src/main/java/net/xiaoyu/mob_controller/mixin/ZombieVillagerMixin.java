package net.xiaoyu.mob_controller.mixin;

import net.xiaoyu.mob_controller.util.MobControlledData;
import net.minecraft.world.entity.monster.ZombieVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ZombieVillager.class)
public class ZombieVillagerMixin {

    @Inject(
        method = "Lnet/minecraft/world/entity/monster/ZombieVillager;finishConversion(Lnet/minecraft/server/level/ServerLevel;)V",
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