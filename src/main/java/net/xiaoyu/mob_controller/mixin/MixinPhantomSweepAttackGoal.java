package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Phantom;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 重定向幻翼内部攻击类（PhantomSweepAttackGoal）stop 方法中的 setTarget(null) 调用。
 * 如果幻翼受控且当前为系统攻击（主人指令），则不清除目标，保持持续追击。
 */
@Mixin(targets = "net.minecraft.world.entity.monster.Phantom$PhantomSweepAttackGoal")
public class MixinPhantomSweepAttackGoal {

    @Redirect(
            method = "stop",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Phantom;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V")
    )
    private void redirectStopSetTarget(Phantom phantom, LivingEntity target) {
        // 仅当不是受控系统攻击时才真正清除目标，否则保持目标
        if (!(MobControlledData.isControlledEntity(phantom) && MobControlledData.isSystemAttack(phantom))) {
            phantom.setTarget(target);
        }
        // 若条件成立，则跳过了 setTarget(null)，幻翼继续保留目标
    }
}