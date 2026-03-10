package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.xiaoyu.mob_controller.entity.IControllableEntity;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntity {
    @SuppressWarnings("ConstantValue")
    @Inject(method = "isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z", at = @At("RETURN"), cancellable = true)
    private void injectIsAlliedTo(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof LivingEntity livingEntity) {
            if (this instanceof IControllableEntity controllable && controllable.isSameTeam(livingEntity)) {
                cir.setReturnValue(true);
            } else if (Mob.class.isInstance(this)) {
                Mob mob = (Mob) (Object) this;
                if (!MobControlUtil.canControlledMobAttackTarget(mob, livingEntity)) {
                    cir.setReturnValue(true);
                }
            }
        }
    }
}
