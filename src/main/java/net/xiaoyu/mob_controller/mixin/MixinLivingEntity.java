package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.xiaoyu.mob_controller.entity.EntityControlledWitch;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {
    @Shadow
    public float yBodyRot;
    @Shadow
    public float yHeadRot;


    public MixinLivingEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @SuppressWarnings("ConstantValue")
    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("RETURN"), cancellable = true)
    private void injectCanAttack(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            if ((Object) (this) instanceof LivingEntity mob) {
                if (MobControlledData.isControlledEntity(mob) && MobControlUtil.isEnemy(mob, target) && !(mob instanceof EntityControlledWitch)) {
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
            if (projectileOwner instanceof LivingEntity) {
                attacker = projectileOwner;
            }
        }

        // 一般情况下的攻击
        if (attacker instanceof LivingEntity mob && MobControlledData.isControlledEntity(mob)) {
            if (!MobControlUtil.isEnemy(mob, livingEntity)) {
                cir.cancel();
            }
        }

        // 其他生物受到被控制生物攻击的反击
        if (attacker instanceof LivingEntity controlledMob && MobControlledData.isControlledEntity(controlledMob)
                && livingEntity instanceof Mob otherMob && !MobControlledData.isControlledEntity(otherMob)) {
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

    @Inject(method = "tickRidden(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void injectTickRidden(Player player, Vec3 travelVector, CallbackInfo ci) {
        Object thiz = this;
        if (thiz instanceof LivingEntity mob) {
            if (mob instanceof Guardian ||
                    mob instanceof Hoglin ||
                    mob instanceof Zoglin ||
                    mob instanceof Ravager ||
                    mob instanceof Cow ||
                    mob instanceof Sheep ||
                    mob instanceof Dolphin) {
                this.setRot(player.getYRot(), player.getXRot() * 0.5F);
                this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
                if (mob instanceof Guardian && !mob.isInWaterOrBubble()) {
                    mob.getControllingPassenger().stopRiding();
                }
            }
        }
    }

    @Inject(method = "getRiddenInput(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void injectGetRiddenInput(Player player, Vec3 travelVector, CallbackInfoReturnable<Vec3> cir) {
        Object thiz = this;
        if (thiz instanceof LivingEntity mob) {
            if (mob instanceof Guardian ||
                    mob instanceof Hoglin ||
                    mob instanceof Zoglin ||
                    mob instanceof Ravager ||
                    mob instanceof Cow ||
                    mob instanceof Sheep ||
                    mob instanceof Dolphin) {
                double x = player.xxa * 0.5;
                double y = 0;
                double z = player.zza;
                if (z <= 0.0) {
                    z *= 0.25;
                }
                if (player instanceof AccessorLivingEntity accessor) {
                    if (mob instanceof Guardian || mob instanceof Dolphin) {
                        if (z != 0) {
                            double i = Math.cos(player.getXRot() * Math.PI / 180.0);
                            double j = -Math.sin(player.getXRot() * Math.PI / 180.0);
                            if (z < 0.0) {
                                i *= -0.5;
                                j *= -0.5;
                            }
                            y = j;
                            z = i;
                        }
                        y += accessor.mob_controller$getJumping() && player.isInWaterOrBubble() ? 0.5 : 0;
                        y *= this.isInWaterOrBubble() ? 1 : 0;
                    } else if (accessor.mob_controller$getJumping() && mob.onGround() && !(mob instanceof PlayerRideableJumping)) {
                        mob.setOnGround(false);
                        double d0 = 0.5 * this.getBlockJumpFactor();
                        double d1 = d0 + (mob.hasEffect(MobEffects.JUMP) ? 0.1 * mob.getEffect(MobEffects.JUMP).getAmplifier() + 1 : 0);
                        Vec3 vec3 = mob.getDeltaMovement();
                        mob.setDeltaMovement(vec3.x, d1, vec3.z);
                        mob.hasImpulse = true;
                        ForgeHooks.onLivingJump(mob);
                    }
                }
                cir.setReturnValue(new Vec3(x, y, z));
            }
        }
    }

    @Inject(method = "getRiddenSpeed(Lnet/minecraft/world/entity/player/Player;)F", at = @At("HEAD"), cancellable = true)
    private void injectGetRiddenSpeed(Player player, CallbackInfoReturnable<Float> cir) {
        Object thiz = this;
        if (thiz instanceof LivingEntity mob) {
            if (mob instanceof Guardian ||
                    mob instanceof Hoglin ||
                    mob instanceof Zoglin ||
                    mob instanceof Ravager ||
                    mob instanceof Cow ||
                    mob instanceof Sheep) {
                cir.setReturnValue((float) mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
            }
            if (mob instanceof Dolphin) {
                cir.setReturnValue((float) (mob.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.1));
            }
        }
    }
}