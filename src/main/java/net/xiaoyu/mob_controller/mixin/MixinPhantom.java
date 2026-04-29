package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.xiaoyu.mob_controller.capability.WaxedCapabilityProvider;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(Phantom.class)
public abstract class MixinPhantom {

    @Unique
    private int mob_controller$followTargetUpdateCooldown = 0;

    /**
     * 在幻翼的 aiStep 尾部修改移动目标点。
     * 仅在受控且为 FOLLOW 模式且无有效战斗目标时生效。
     */
    @Inject(method = "aiStep", at = @At("TAIL"))
    private void onAiStep(CallbackInfo ci) {
        Phantom phantom = (Phantom) (Object) this;

        // 仅服务端执行
        if (phantom.level().isClientSide) return;

        // 检查是否被本模组控制
        if (!MobControlledData.isControlledEntity(phantom)) return;

        // 获取控制模式
        MobControlledData.ControlMode mode = MobControlledData.getControlMode(phantom);
        if (mode != MobControlledData.ControlMode.FOLLOW) return;

        // 获取控制者
        Player controller = MobControlledData.getController(phantom, phantom.level());
        if (controller == null) return;

        // 检查当前是否有战斗目标（护主/索敌模式下的敌人）
        LivingEntity combatTarget = phantom.getTarget();
        boolean hasValidCombatTarget = combatTarget != null && combatTarget.isAlive();

        // 如果有战斗目标，让幻翼正常攻击，不干扰原版AI
        if (hasValidCombatTarget) {
            // 同时清除可能残留的跟随冷却（防止冷却干扰）
            mob_controller$followTargetUpdateCooldown = 0;
            return;
        }

        // 无战斗目标：强制跟随主人，围绕主人盘旋避免抽搐
        if (mob_controller$followTargetUpdateCooldown > 0) {
            mob_controller$followTargetUpdateCooldown--;
            return;
        }
        mob_controller$followTargetUpdateCooldown = 10; // 每 10 刻更新一次目标点

        net.minecraft.util.RandomSource random = phantom.getRandom();
        double angle = random.nextDouble() * 2 * Math.PI;
        double radius = 4.0 + random.nextDouble() * 4.0;
        double offsetX = Math.cos(angle) * radius;
        double offsetZ = Math.sin(angle) * radius;
        double offsetY = 2.0 + random.nextDouble() * 3.0;

        PhantomAccessor accessor = (PhantomAccessor) phantom;
        accessor.setMoveTargetPoint(new Vec3(
                controller.getX() + offsetX,
                controller.getY() + offsetY,
                controller.getZ() + offsetZ
        ));

        // 清除可能残留的仇恨目标
        if (phantom.getTarget() != null) {
            phantom.setTarget(null);
        }
    }

    /**
     * 拦截白天着火的逻辑（打蜡后不燃烧）
     */
    @Inject(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/monster/Phantom;setSecondsOnFire(I)V"
            ),
            cancellable = true
    )
    private void onSetSecondsOnFire(CallbackInfo ci) {
        Phantom phantom = (Phantom) (Object) this;
        phantom.getCapability(WaxedCapabilityProvider.WAXED_CAPABILITY)
                .ifPresent(cap -> {
                    if (cap.isWaxed()) {
                        ci.cancel();
                    }
                });
    }
}