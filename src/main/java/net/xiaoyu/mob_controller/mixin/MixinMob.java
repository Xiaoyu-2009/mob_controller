package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.level.Level;
import net.xiaoyu.mob_controller.entity.IControllableEntity;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MixinMob extends LivingEntity implements Targeting {
    protected MixinMob(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;

        // 不会进行转换的被控制生物
        if (MobControlledData.isControlledEntity(mob)) {
            // 猪灵/疣猪兽=僵尸猪灵/僵尸疣猪兽
            if (mob instanceof AbstractPiglin) {
                ((AbstractPiglin) mob).setImmuneToZombification(true);
            } else if (mob instanceof Hoglin) {
                ((Hoglin) mob).setImmuneToZombification(true);
            }

            // 骷髅=流浪者
            if (mob instanceof Skeleton skeleton) {
                skeleton.setFreezeConverting(false);
            }
        }

        if (MobControlledData.getControlMode(mob) == MobControlledData.ControlMode.FOLLOW) {
            // 传送/跟随
            MobControlUtil.handleMobFollowing(mob);
        }
    }

    /**
     * 被控制的生物/其他生物中立
     */
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void onSetTarget(LivingEntity target, CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;

        if (MobControlledData.isControlledEntity(mob)) {
            if (!MobControlledData.isSystemAttack(mob)) {
                if (mob instanceof IControllableEntity controllable) {
                    if (!controllable.canSeeAsTarget(target)) {
                        ci.cancel();
                    }
                    return;
                }
                ci.cancel();
            } /*else {
                MobControlledData.clearSystemAttack(mob);
            }*/
        } else if (target != null && MobControlledData.isControlledEntity(target)) {
            if (!(mob.getLastHurtByMob() != null && MobControlledData.isControlledEntity(mob.getLastHurtByMob()))) {
                if (mob instanceof IControllableEntity controllable) {
                    if (!controllable.canSeeAsTarget(target)) {
                        ci.cancel();
                    }
                    return;
                }
                ci.cancel();
            }
        }
    }

    /**
     * 被控制的远古守卫者/守卫者攻击解除限制
     */
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void onSetTargetForGuardian(LivingEntity target, CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;

        if (mob instanceof Guardian guardian) {

            if (MobControlledData.isControlledEntity(guardian) && target == null) {
                LivingEntity currentTarget = guardian.getTarget();
                if (currentTarget != null && currentTarget.isAlive() && !currentTarget.isDeadOrDying()) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "getControllingPassenger()Lnet/minecraft/world/entity/LivingEntity;", at = @At("RETURN"), cancellable = true)
    private void injectGetControllingPassenger(CallbackInfoReturnable<LivingEntity> cir) {
        Object thiz = this;
        if (thiz instanceof Mob mob) {
            Entity entity = this.getFirstPassenger();
            if (entity != null && MobControlledData.isControlledEntity(mob) && MobControlledData.getControllerUUID(mob).equals(entity.getUUID())) {
                if (entity instanceof LivingEntity living) {
                    cir.setReturnValue(living);
                }
            }
        }
    }
}