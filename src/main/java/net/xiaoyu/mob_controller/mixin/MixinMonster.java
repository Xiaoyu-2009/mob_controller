package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 怪物休息干扰判定注入。
 *
 * <p>受控怪物不会阻止玩家睡觉。</p>
 */
@Mixin(Monster.class)
public class MixinMonster {

    /**
     * 注入 {@code isPreventingPlayerRest} 头部：受控状态返回 false。
     */
    @Inject(method = "isPreventingPlayerRest", at = @At("HEAD"), cancellable = true)
    private void onIsPreventingPlayerRest(Player player, CallbackInfoReturnable<Boolean> cir) {
        Monster thiz = (Monster) (Object) this;
        if (MobControlledData.isControlledEntity(thiz)) {
            cir.setReturnValue(false);
        }
    }
}
