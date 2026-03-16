package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Warden.class)
public class WardenMixin {

    @Inject(method = "canTargetEntity", at = @At("HEAD"), cancellable = true)
    private void targetWarden(@Nullable Entity entity, CallbackInfoReturnable<Boolean> info) {
        Warden warden = (Warden) (Object) this;

        if (entity instanceof LivingEntity livingEntity) {

            // 被控制的坚守者取消攻击欲望
            if (MobControlledData.isControlledEntity(warden)) {
                if (!MobControlUtil.isEnemy(warden, livingEntity)) {
                    info.cancel();
                }
            }
        }
    }
}