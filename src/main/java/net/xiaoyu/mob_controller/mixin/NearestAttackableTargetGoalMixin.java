package net.xiaoyu.mob_controller.mixin;

import net.xiaoyu.mob_controller.util.MobControlledData;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(NearestAttackableTargetGoal.class)
public class NearestAttackableTargetGoalMixin {
    
    // 被控制的史莱姆不攻击主人
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void excludeOwnerFromTargeting(CallbackInfoReturnable<Boolean> cir) {
        NearestAttackableTargetGoal<?> instance = (NearestAttackableTargetGoal<?>) (Object) this;

        if (MobControlledData.isControlledMob(instance.mob)) {
            if (instance.target instanceof Player) {
                Player player = (Player) instance.target;

                if (player.getUUID().equals(MobControlledData.getControllerUUID(instance.mob))) {
                    cir.cancel();
                }
            }
        }
    }
}