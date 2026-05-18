// 路径：net.xiaoyu.mob_controller.mixin.RisingVehicleMixin
package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.xiaoyu.mob_controller.util.MobControlledData;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.RideSpeedConfigCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class RisingVehicleMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        // 1. 检查是否有乘客且第一位是玩家
        if (self.getPassengers().isEmpty()) return;
        Entity firstPassenger = self.getFirstPassenger();
        if (!(firstPassenger instanceof Player player)) return;

        // 2. 仅处理受控生物且骑手为控制者
        if (!(self instanceof Mob mob)) return;
        if (!MobControlledData.isControlledEntity(mob)) return;
        if (!player.getUUID().equals(MobControlledData.getControllerUUID(mob))) return;

        // 3. 如果没有自定义速度配置，则跳过
        if (!RideSpeedConfigCache.hasCustomRideSpeed(mob)) return;

        // 4. 确定接管类型
        MobControlUtil.RideableType type = MobControlUtil.getRideableType(mob);
        if (type == null) return;

        boolean takeOver = switch (type) {
            case FLYING -> true;
            case AQUATIC, AMPHIBIAN -> mob.isInWaterOrBubble();
            case FLYING_LAND -> !mob.onGround();
            default -> false;
        };
        if (!takeOver) return;

        // 5. 获取自定义速度
        double speed = RideSpeedConfigCache.getCustomRideSpeed(mob);
        float strafe = player.xxa;
        float forward = player.zza;
        boolean isJumping = ((AccessorLivingEntity) player).mob_controller$getJumping();

        // 6. 计算水平移动
        float yaw = player.getYRot();
        double rad = Math.toRadians(yaw);
        double moveX = (-Math.sin(rad) * forward + Math.cos(rad) * strafe) * speed;
        double moveZ = ( Math.cos(rad) * forward + Math.sin(rad) * strafe) * speed;

        // 7. 垂直移动
        double moveY = 0.0;
        boolean isAquaticOrAmphibianInWater = (type == MobControlUtil.RideableType.AQUATIC) ||
                (type == MobControlUtil.RideableType.AMPHIBIAN && mob.isInWaterOrBubble());

        if (isJumping) {
            moveY = speed;
        } else if (forward > 0) {
            double pitchRad = Math.toRadians(player.getXRot());
            moveY = -Math.sin(pitchRad) * speed;
        }
        if (moveY > speed) moveY = speed;
        if (moveY < -speed) moveY = -speed;

        // 8. 应用新速度
        self.setDeltaMovement(moveX, moveY, moveZ);
        self.hasImpulse = true;
    }
}