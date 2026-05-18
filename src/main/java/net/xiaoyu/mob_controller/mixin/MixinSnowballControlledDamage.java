package net.xiaoyu.mob_controller.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.phys.EntityHitResult;
import net.xiaoyu.mob_controller.config.FeatureConfig;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Snowball.class)
public abstract class MixinSnowballControlledDamage {

    private static final float MIN_DAMAGE = 0.000001F;

    /**
     * 根据配置决定是否对雪球伤害进行特殊处理（极小伤害+回血）。
     */
    @WrapOperation(
            method = "onHitEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z")
    )
    private boolean modifySnowballDamage(Entity target, DamageSource source, float originalDamage,
                                         Operation<Boolean> original, EntityHitResult hitResult) {
        Snowball snowball = (Snowball) (Object) this;
        Entity shooter = snowball.getOwner();

        FeatureConfig.SnowballKnockbackOption mode = FeatureConfig.SNOWBALL_KNOCKBACK_MODE.get();
        boolean applyEffect = false;

        switch (mode) {
            case DISABLED:
                applyEffect = false;
                break;
            case CONTROLLED_SNOW_GOLEM_ONLY:
                if (shooter instanceof SnowGolem golem && MobControlledData.isControlledEntity(golem)) {
                    applyEffect = true;
                }
                break;
            case SNOW_GOLEM_ONLY:
                if (shooter instanceof SnowGolem) {
                    applyEffect = true;
                }
                break;
            case ALL_SNOWBALLS:
                applyEffect = true;
                break;
        }

        if (applyEffect && target instanceof Player player) {
            boolean hurtResult = original.call(target, source, MIN_DAMAGE);
            if (hurtResult && !target.level().isClientSide) {
                player.heal(MIN_DAMAGE);
            }
            return hurtResult;
        }
        return original.call(target, source, originalDamage);
    }

    /**
     * 着火雪球使目标着火（此效果不受配置影响，保持独立）
     */
    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void setFireOnHit(EntityHitResult hitResult, CallbackInfo ci) {
        if (!FeatureConfig.SNOWBALL_FIRE_HIT.get()) {
            return;
        }
        Snowball snowball = (Snowball) (Object) this;
        if (snowball.isOnFire()) {
            Entity target = hitResult.getEntity();
            if (target instanceof LivingEntity living) {
                living.setSecondsOnFire(5);
            }
        }
    }
}