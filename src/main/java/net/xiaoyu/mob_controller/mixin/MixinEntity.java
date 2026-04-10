package net.xiaoyu.mob_controller.mixin;

import net.minecraft.commands.CommandSource;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraftforge.common.extensions.IForgeEntity;
import net.xiaoyu.mob_controller.entity.EntityControlledWitch;
import net.xiaoyu.mob_controller.entity.IControllableEntity;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
/**
 * 实体通用行为注入。
 *
 * <p>扩展阵营判定、骑乘偏移与控制者骑乘控制逻辑。</p>
 */
@Mixin(Entity.class)
public abstract class MixinEntity implements Nameable, EntityAccess, CommandSource, IForgeEntity {
    /**
     * 注入 {@code isAlliedTo} 返回点：为受控实体追加同阵营判定。
     */
    @SuppressWarnings("ConstantValue")
    @Inject(method = "isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z", at = @At("RETURN"), cancellable = true)
    private void injectIsAlliedTo(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof LivingEntity livingEntity) {
            if (this instanceof IControllableEntity controllable && controllable.isSameTeam(livingEntity)) {
                cir.setReturnValue(true);
            } else if (LivingEntity.class.isInstance(this)) {
                LivingEntity mob = (LivingEntity) (Object) this;
                if (MobControlledData.isControlledEntity(mob) && !MobControlUtil.isEnemy(mob, livingEntity) && !(mob instanceof EntityControlledWitch)) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    /**
     * 注入 {@code getPassengersRidingOffset} 返回点：受控远古守卫者提升骑乘偏移。
     */
    @SuppressWarnings("ConstantValue")
    @Inject(method = "getPassengersRidingOffset()D", at = @At("RETURN"), cancellable = true)
    private void injectGetPassengersRidingOffset(CallbackInfoReturnable<Double> cir) {
        if (ElderGuardian.class.isInstance(this)) {
            ElderGuardian mob = (ElderGuardian) (Object) this;
            if (MobControlledData.isControlledEntity(mob)) {
                cir.setReturnValue(cir.getReturnValue() * 1.4);
            }
        }
    }

    /**
     * 注入 {@code getControllingPassenger} 返回点：允许控制者成为骑乘操作者。
     */
    @Inject(method = "getControllingPassenger()Lnet/minecraft/world/entity/LivingEntity;", at = @At("RETURN"), cancellable = true)
    private void injectGetControllingPassenger(CallbackInfoReturnable<LivingEntity> cir) {
        Object thiz = this;
        if (thiz instanceof LivingEntity mob) {
            Entity entity = this.getFirstPassenger();
            if (entity != null && MobControlledData.isControlledEntity(mob) && MobControlledData.getControllerUUID(mob).equals(entity.getUUID())) {
                if (entity instanceof LivingEntity living) {
                    cir.setReturnValue(living);
                }
            }
        }
    }

    @Shadow
    @Nullable
    public abstract Entity getFirstPassenger();
}
