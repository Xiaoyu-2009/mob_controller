package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Targeting;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.xiaoyu.mob_controller.entity.IControllableEntity;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 生物基础行为注入。
 *
 * <p>处理受控生物每刻行为、目标选择限制与骑乘控制相关逻辑。</p>
 */

@Mixin(Mob.class)
public abstract class MixinMob extends LivingEntity implements Targeting {
    protected MixinMob(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * 注入 {@code tick} 头部：处理受控生物免转换、跟随/停留逻辑与坐标焊死。
     */
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

        if (!mob.level().isClientSide) {
            MobControlledData.ControlMode mode = MobControlledData.getControlMode(mob);

            if (mode == MobControlledData.ControlMode.FOLLOW) {
                // 传送/跟随
                MobControlUtil.handleMobFollowing(mob);
                MobControlUtil.clearStayFlightCoordinateWeld(mob);
            } else if (mode == MobControlledData.ControlMode.STAY && MobControlledData.isControlledEntity(mob)
                       && MobControlUtil.shouldUseStayFlightWeld(mob)) {
                // 特殊 AI 生物停留时坐标焊死
                MobControlUtil.applyStayFlightCoordinateWeld(mob);
            } else {
                MobControlUtil.clearStayFlightCoordinateWeld(mob);
            }
        }
    }

    /**
     * 注入 {@code setTarget} 头部：限制受控生物与其他生物对目标的错误锁定。
     */
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void onSetTarget(LivingEntity target, CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;

        if (MobControlledData.isControlledEntity(mob)) {
            // Brain 类生物（Piglin / Hoglin / Zoglin / Warden）按原版逻辑攻击玩家，
            // 仅阻止攻击控制者本人。
            if (mob instanceof AbstractPiglin || mob instanceof Hoglin
                    || mob instanceof Zoglin || mob instanceof Warden) {
                if (target instanceof Player && MobControlUtil.isController(mob, target)) {
                    ci.cancel();
                }
                return;
            }

            if (target instanceof Player
                && !MobControlUtil.canKeepCombatTarget(mob, target)) {
                ci.cancel();
                return;
            }

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
     * 注入 {@code setTarget} 头部（守卫者特化）：保留有效光束目标。
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

    /**
     * 注入 {@code getControllingPassenger} 返回点：允许控制者作为骑乘操作者。
     */
    @Inject(method = "getControllingPassenger()Lnet/minecraft/world/entity/LivingEntity;", at = @At("RETURN"), cancellable = true)
    private void injectGetControllingPassenger(CallbackInfoReturnable<LivingEntity> cir) {
        Object thiz = this;
        if (thiz instanceof Mob mob) {
            Entity entity = this.getFirstPassenger();
            if (entity != null && MobControlledData.isControlledEntity(mob) && MobControlledData.getControllerUUID(mob)
                .equals(entity.getUUID())) {
                if (entity instanceof LivingEntity living) {
                    cir.setReturnValue(living);
                }
            }
        }
    }
}
