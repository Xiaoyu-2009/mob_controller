package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.*;
import net.xiaoyu.mob_controller.util.*;
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
}