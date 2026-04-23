package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.player.Player;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * 僵尸疣猪兽行为注入。
 *
 * <p>受控僵尸疣猪兽不会攻击控制者，并清空自动寻敌结果。</p>
 */
@Mixin(Zoglin.class)
public class MixinZoglin {
    /**
     * 注入 {@code doHurtTarget} 头部：目标为控制者时取消攻击。
     */
    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void onDoHurtTarget(Entity target, CallbackInfoReturnable<Boolean> cir) {
        Zoglin zoglin = (Zoglin) (Object) this;

        if (MobControlledData.isControlledEntity(zoglin)) {
            // 默认中立：仅在系统攻击（护主/反击/主人指令）期间允许造成伤害。
            if (!MobControlledData.isSystemAttack(zoglin)) {
                cir.setReturnValue(false);
                return;
            }

            if (target instanceof Player) {
                if (target.getUUID().equals(MobControlledData.getControllerUUID(zoglin))) {
                    cir.cancel();
                }
            }
        }
    }

    /**
     * 注入 {@code findNearestValidAttackTarget} 返回点：受控状态下过滤控制者本人，其余目标按原版逻辑。
     */
    @Inject(method = "findNearestValidAttackTarget()Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private void injectFindNearestValidAttackTarget(CallbackInfoReturnable<Optional<? extends LivingEntity>> cir) {
        Zoglin zoglin = (Zoglin) (Object) this;
        if (MobControlledData.isControlledEntity(zoglin)) {
            Optional<? extends LivingEntity> result = cir.getReturnValue();
            if (result.isPresent()
                && (!MobControlledData.isSystemAttack(zoglin)
                    || !MobControlUtil.canKeepCombatTarget(zoglin, result.get())
                    || MobControlUtil.isController(zoglin, result.get()))) {
                cir.setReturnValue(Optional.empty());
            }
        }
    }
}
