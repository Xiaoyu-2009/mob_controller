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

import java.util.UUID;

/**
 * 史莱姆行为注入。
 *
 * <p>处理受控史莱姆分裂继承、接触玩家与碰撞目标时的敌友规则。</p>
 */
@Mixin(Slime.class)
public abstract class SlimeMixin {
    /**
     * 注入 {@code remove} 分裂流程：将新分裂史莱姆继承为同控制者受控状态。
     */
    @Inject(
        method = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Slime;moveTo(DDDFF)V")
    )
    private void injectRemove(Entity.RemovalReason reason, CallbackInfo ci, @Local Slime slime) {
        Mob mob = (Mob) (Object) this;
        if (MobControlledData.isControlledEntity(mob)) {
            UUID controllerUUID = MobControlledData.getControllerUUID(mob);
            if (controllerUUID != null) {
                MobControlledData.addControlledMob(controllerUUID, slime);
            }
        }
    }

    /**
     * 注入 {@code playerTouch} 头部：控制者免疫接触伤害，敌对玩家触发反击。
     */
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void onPlayerTouch(Player player, CallbackInfo ci) {
        Slime slime = (Slime) (Object) this;

        if (MobControlledData.isControlledEntity(slime)) {
            if (!player.getUUID().equals(MobControlledData.getControllerUUID(slime))) {
                if (!MobControlUtil.isEnemy(slime, player)) {
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

    /**
     * 注入 {@code push} 头部：根据敌友关系决定是否触发碰撞攻击。
     */
    @Inject(method = "push", at = @At("HEAD"), cancellable = true)
    private void onPush(Entity entity, CallbackInfo ci) {
        Slime slime = (Slime) (Object) this;

        if (MobControlledData.isControlledEntity(slime)) {
            if (entity instanceof LivingEntity target) {

                // 不攻击主人
                if (target instanceof Player) {
                    if (target.getUUID().equals(MobControlledData.getControllerUUID(slime))) {
                        ci.cancel();
                        return;
                    }
                }

                if (!MobControlUtil.isEnemy(slime, target)) {
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
