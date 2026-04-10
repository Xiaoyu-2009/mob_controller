package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/**
 * 疣猪兽攻击行为注入。
 *
 * <p>受控疣猪兽不会伤害控制者或友方目标。</p>
 */
@Mixin(Hoglin.class)
public class HoglinMixin {
    /**
     * 注入 {@code doHurtTarget} 头部：非敌对目标时取消伤害。
     */
    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void onDoHurtTarget(Entity target, CallbackInfoReturnable<Boolean> cir) {
        Hoglin hoglin = (Hoglin) (Object) this;
        if (MobControlledData.isControlledEntity(hoglin) && !MobControlUtil.isEnemy(hoglin, target)) {
            cir.cancel();
        }
    }
}
