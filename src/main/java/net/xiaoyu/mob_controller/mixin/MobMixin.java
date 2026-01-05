package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.*;
import net.xiaoyu.mob_controller.util.*;
import net.minecraft.world.entity.monster.Guardian;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@Mixin(Mob.class)
public class MobMixin {
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        
        // 被控制的生物
        if (MobControlledData.isControlledMob(mob)) {
            // 不会进行转换
            try {
                Method method = mob.getClass().getMethod("setImmuneToZombification", boolean.class);
                method.invoke(mob, true);
            } catch (Exception e) {}
        }

        if (MobControlledData.getControlMode(mob) == MobControlledData.ControlMode.FOLLOW) {
            // 传送/跟随
            MobControlUtil.handleMobFollowing(mob);
        }
    }

    // 被控制的生物/其他生物中立
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void onSetTarget(LivingEntity target, CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;

        if (MobControlledData.isControlledMob(mob)) {
            if (!MobControlledData.isSystemAttack(mob)) {
                ci.cancel();
            } else {
                /*MobControlledData.clearSystemAttack(mob);*/
            }
        } else if (MobControlledData.isControlledEntity(target)) {
            if (!(mob.getLastHurtByMob() != null && MobControlledData.isControlledEntity(mob.getLastHurtByMob()))) {
                ci.cancel();
            }
        }
    }

    // 被控制的远古守卫者/守卫者攻击解除限制
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void onSetTargetForGuardian(LivingEntity target, CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;

        if (mob instanceof Guardian) {
            Guardian guardian = (Guardian) mob;

            if (MobControlledData.isControlledMob(guardian) && target == null) {
                LivingEntity currentTarget = guardian.getTarget();
                if (currentTarget != null && currentTarget.isAlive() && !currentTarget.isDeadOrDying()) {
                    ci.cancel();
                }
            }
        }
    }
}