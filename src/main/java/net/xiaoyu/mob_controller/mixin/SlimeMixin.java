package net.xiaoyu.mob_controller.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Slime.class)
public abstract class SlimeMixin {
    @Inject(method = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Slime;moveTo(DDDFF)V"))
    private void injectRemove(Entity.RemovalReason reason, CallbackInfo ci, @Local Slime slime) {
        Mob mob = (Mob) (Object) this;
        if (MobControlledData.isControlledMob(mob)) {
            MobControlledData.addControlledMob(MobControlledData.getControllerUUID(mob), slime);
        }
    }

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
            if (entity instanceof LivingEntity target) {

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