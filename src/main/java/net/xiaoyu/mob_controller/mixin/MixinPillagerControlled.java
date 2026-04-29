package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.monster.Pillager;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Pillager.class)
public abstract class MixinPillagerControlled {

    @Inject(method = "isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void onIsAlliedTo(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        Pillager pillager = (Pillager) (Object) this;
        if (MobControlledData.isControlledEntity(pillager)) {
            if (entity instanceof net.minecraft.world.entity.LivingEntity living
                    && living.getMobType() == MobType.ILLAGER) {
                cir.setReturnValue(false);
            }
        }
    }
}