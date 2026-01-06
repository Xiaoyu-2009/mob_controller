package net.xiaoyu.mob_controller.mixin;

import net.xiaoyu.mob_controller.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Slime.class)
public abstract class SlimeMixin {

    // 被控制的史莱姆接触玩家
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void onPlayerTouch(Player player, CallbackInfo ci) {
        Slime slime = (Slime) (Object) this;
        
        if (MobControlledData.isControlledMob(slime)) {
            if (!player.getUUID().equals(MobControlledData.getControllerUUID(slime))) {
                if (!MobControlUtil.canControlledMobAttackTarget(slime, player)) {
                    ci.cancel();
                }
                
                MobControlledData.markSystemAttack(slime);
                MobControlUtil.setMobTargetWithAnger(slime, player);
            } else {
                // 不攻击主人
                ci.cancel();
            }
        }
    }

    // 被控制的史莱姆撞到其他实体
    @Inject(method = "push", at = @At("HEAD"), cancellable = true)
    private void onPush(Entity entity, CallbackInfo ci) {
        Slime slime = (Slime) (Object) this;
        
        if (MobControlledData.isControlledMob(slime)) {
            if (entity instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) entity;
                
                // 不攻击主人
                if (target instanceof Player) {
                    if (target.getUUID().equals(MobControlledData.getControllerUUID(slime))) {
                        ci.cancel();
                        return;
                    }
                }

                if (!MobControlUtil.canControlledMobAttackTarget(slime, target)) {
                    ci.cancel();
                    return;
                }
                
                MobControlledData.markSystemAttack(slime);
                MobControlUtil.setMobTargetWithAnger(slime, target);
            } else {
                ci.cancel();
            }
        }
    }
}