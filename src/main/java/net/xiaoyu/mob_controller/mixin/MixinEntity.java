package net.xiaoyu.mob_controller.mixin;

import net.minecraft.commands.CommandSource;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.extensions.IForgeEntity;
import net.xiaoyu.mob_controller.entity.IControllableEntity;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
                if (MobControlledData.isControlledEntity(mob) && !MobControlUtil.canKeepCombatTarget(
                    mob,
                    livingEntity
                )) {
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
            if (entity != null && MobControlledData.isControlledEntity(mob) && MobControlledData.getControllerUUID(mob)
                .equals(entity.getUUID())) {
                if (entity instanceof LivingEntity living) {
                    cir.setReturnValue(living);
                }
            }
        }
    }

    @Shadow
    @Nullable
    public abstract Entity getFirstPassenger();

    /**
     * 拦截 positionRider 方法，在设置乘客位置时应用偏移。
     * 该方法会在每次坐骑刷新乘客位置时调用，稳定性好，不存在混淆映射问题。
     *
     * @param passenger 乘客实体
     * @param function  移动函数（通常使用 Entity::setPos）
     * @param ci        回调信息
     */
    @Inject(
            method = "positionRider(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity$MoveFunction;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = true
    )
    private void onPositionRider(Entity passenger, Entity.MoveFunction function, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Mob mount && MobControlUtil.isDirectRideableControlledMob(mount)) {
            Vec3 offset = MobControlUtil.getRideOffset(mount);
            if (!offset.equals(Vec3.ZERO)) {
                // 获取默认位置
                Vec3 defaultPos = getDefaultPassengerPosition(mount, passenger);
                // 计算旋转后的偏移（只旋转 X 和 Z，Y 不变）
                float yaw = mount.getYRot();
                double rad = Math.toRadians(yaw);
                double cos = Math.cos(rad);
                double sin = Math.sin(rad);
                double rotatedX = offset.x * cos - offset.z * sin;
                double rotatedZ = offset.x * sin + offset.z * cos;
                // 最终位置 = 默认位置 + 旋转后的偏移
                function.accept(passenger,
                        defaultPos.x + rotatedX,
                        defaultPos.y + offset.y,
                        defaultPos.z + rotatedZ);
                ci.cancel();
            }
        }
    }

    private Vec3 getDefaultPassengerPosition(Entity mount, Entity passenger) {
        double offsetY = mount.getPassengersRidingOffset() + passenger.getMyRidingOffset();
        Vec3 localOffset = new Vec3(0.0, offsetY, 0.0);
        float yaw = mount.getYRot();
        float rad = -yaw * (float) (Math.PI / 180.0);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        double rotatedX = localOffset.x * cos - localOffset.z * sin;
        double rotatedZ = localOffset.x * sin + localOffset.z * cos;
        return new Vec3(mount.getX() + rotatedX, mount.getY() + localOffset.y, mount.getZ() + rotatedZ);
    }
}
