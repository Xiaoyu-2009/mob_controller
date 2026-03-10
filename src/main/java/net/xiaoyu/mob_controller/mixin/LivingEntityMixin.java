package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.xiaoyu.mob_controller.entity.IControllableEntity;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @SuppressWarnings("ConstantValue")
    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("RETURN"), cancellable = true)
    private void injectCanAttack(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            if (this instanceof IControllableEntity controllable && !controllable.wantsToAttack(target)) {
                cir.cancel();
            }
            if ((Object) (this) instanceof Mob mob && MobControlledData.isControlledMob(mob)) {
                if (target.getUUID().equals(MobControlledData.getControllerUUID(mob))) {
                    cir.cancel();
                }
            }
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        Entity attacker = source.getEntity();

        // 投射物
        if (source.getDirectEntity() instanceof Projectile projectile) {
            Entity projectileOwner = projectile.getOwner();
            if (projectileOwner instanceof Mob) {
                attacker = projectileOwner;
            }
        }

        // 一般情况下的攻击
        if (attacker instanceof Mob mob && MobControlledData.isControlledMob(mob)) {
            if (!MobControlUtil.canControlledMobAttackTarget(mob, livingEntity)) {
                cir.cancel();
            }
        }

        // 其他生物受到被控制生物攻击的反击
        if (attacker instanceof Mob controlledMob && MobControlledData.isControlledMob(controlledMob)
                && livingEntity instanceof Mob otherMob && !MobControlledData.isControlledMob(otherMob)) {
            // 排除创造/旁观者模式
            if (source.getEntity() instanceof Player player) {
                if (player.isCreative() || player.isSpectator()) {
                    return;
                }
            }

            MobControlledData.markSystemAttack(otherMob);
            otherMob.setTarget(controlledMob);
        }
    }
}