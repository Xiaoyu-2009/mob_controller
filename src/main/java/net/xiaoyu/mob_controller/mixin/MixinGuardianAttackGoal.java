package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Player;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.monster.Guardian$GuardianAttackGoal")
public class MixinGuardianAttackGoal {
    @Shadow
    @Final
    private Guardian guardian;

    @Inject(method = "canContinueToUse()Z", at = @At("RETURN"), cancellable = true)
    private void injectCanContinueToUse(CallbackInfoReturnable<Boolean> cir) {
        if (!MobControlledData.isControlledEntity(guardian)) return;
        // 原版返回 false，无需覆盖
        if (!cir.getReturnValue()) return;

        LivingEntity target = guardian.getTarget();
        if (target == null) return;

        // 对于非玩家目标，使用原版 isEnemy 判定（等价于 MobControlUtil.isEnemy）
        if (!(target instanceof Player)) {
            if (!MobControlUtil.isEnemy(guardian, target)) {
                cir.setReturnValue(false);
            }
            return;
        }

        // 玩家目标：必须允许攻击（主人指令/反击）
        Player playerTarget = (Player) target;
        boolean allowAttack = MobControlUtil.canAttackPlayerByOwnerCommand(guardian, playerTarget)
                || MobControlUtil.canRetaliateAgainst(guardian, playerTarget)
                || MobControlUtil.canRetaliateAgainstImmediateAttacker(guardian, playerTarget);
        if (!allowAttack) {
            cir.setReturnValue(false);
        }
    }
}