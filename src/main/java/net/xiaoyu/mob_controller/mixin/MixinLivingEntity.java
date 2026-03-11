package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
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
            if ((Object) (this) instanceof Mob mob) {
                if (MobControlledData.isControlledEntity(mob) && MobControlUtil.canControlledMobAttackTarget(mob, target)) {
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

    @Inject(method = "tickRidden(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void injectTickRidden(Player player, Vec3 travelVector, CallbackInfo ci) {
        Object thiz = this;
        if (thiz instanceof Mob mob) {
            if (mob instanceof Guardian ||
                    mob instanceof Hoglin ||
                    mob instanceof Zoglin ||
                    mob instanceof Ravager) {
                Vec2 vec2 = new Vec2(player.getXRot() * 0.5F, player.getYRot());
                this.setRot(vec2.y, vec2.x);
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
        if (thiz instanceof Mob mob) {
            if (mob instanceof Guardian ||
                    mob instanceof Hoglin ||
                    mob instanceof Zoglin ||
                    mob instanceof Ravager) {
                double x = player.xxa * 0.5;
                double y = 0;
                double z = player.zza;
                if (z <= 0.0) {
                    z *= 0.25;
                }

                if (mob instanceof Guardian && player instanceof AccessorLivingEntity accessor) {
                    y = accessor.mob_controller$getJumping() && player.isInWaterOrBubble() ? 0.3 : -0.1;
                }
                cir.setReturnValue(new Vec3(x, y, z));
            }
        }
    }

    @Inject(method = "getRiddenSpeed(Lnet/minecraft/world/entity/player/Player;)F", at = @At("HEAD"), cancellable = true)
    private void injectGetRiddenSpeed(Player player, CallbackInfoReturnable<Float> cir) {
        Object thiz = this;
        if (thiz instanceof Mob mob) {
            if (mob instanceof Guardian ||
                    mob instanceof Hoglin ||
                    mob instanceof Zoglin ||
                    mob instanceof Ravager) {
                cir.setReturnValue((float) mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
            }
        }
    }
}