package net.xiaoyu.mob_controller.mixin;

import fuzs.mutantmonsters.world.entity.mutant.MutantSnowGolem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MutantSnowGolem.class)
public class MixinMutantSnowGolem {

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void onMobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        MutantSnowGolem golem = (MutantSnowGolem) (Object) this;

        // 检查是否被本模组控制，且当前玩家是控制者，且玩家正在潜行
        if (MobControlledData.isControlledEntity(golem) &&
                player.getUUID().equals(MobControlledData.getControllerUUID(golem)) &&
                player.isShiftKeyDown()) {
            // 阻止原模组的切换主人逻辑（返回 PASS，表示未处理此交互）
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}