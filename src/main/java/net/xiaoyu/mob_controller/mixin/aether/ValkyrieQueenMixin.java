package net.xiaoyu.mob_controller.mixin.aether;

import com.aetherteam.aether.entity.monster.dungeon.boss.ValkyrieQueen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(ValkyrieQueen.class)
public class ValkyrieQueenMixin {

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void onMobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ValkyrieQueen queen = (ValkyrieQueen) (Object) this;

        // 如果女王已被当前玩家控制，则阻止弹出对话
        if (MobControlledData.isControlledEntity(queen) &&
                Objects.equals(MobControlledData.getControllerUUID(queen), player.getUUID())) {
            // 直接返回 PASS，不影响其他物品的交互（如装备编辑蓝图）
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}