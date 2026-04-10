package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Monster.class)
public class MixinMonster {

    /**
     * 被控制的生物不会干扰玩家睡觉
     */
    @Inject(method = "isPreventingPlayerRest", at = @At("HEAD"), cancellable = true)
    private void onIsPreventingPlayerRest(Player player, CallbackInfoReturnable<Boolean> cir) {
        Monster thiz = (Monster) (Object) this;
        if (MobControlledData.isControlledEntity(thiz)) {
            cir.setReturnValue(false);
        }
    }
}

