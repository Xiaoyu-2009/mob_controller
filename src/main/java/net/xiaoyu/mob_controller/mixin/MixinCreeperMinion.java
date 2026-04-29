package net.xiaoyu.mob_controller.mixin;

import fuzs.mutantmonsters.init.ModRegistry;
import fuzs.mutantmonsters.world.entity.CreeperMinion;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreeperMinion.class)
public class MixinCreeperMinion {

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void onMobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        CreeperMinion minion = (CreeperMinion) (Object) this;

        // 仅当苦力怕仆从已被本模组控制，且当前玩家是控制者，且玩家正在潜行时进行拦截
        if (MobControlledData.isControlledEntity(minion) &&
                player.getUUID().equals(MobControlledData.getControllerUUID(minion)) &&
                player.isShiftKeyDown()) {

            ItemStack stack = player.getItemInHand(hand);

            // 如果手持物品不是追踪器、火药、TNT（这三种是原模组的特殊交互物品），则阻止此次交互（防止切换坐下/站起）
            if (!stack.is(ModRegistry.CREEPER_MINION_TRACKER_ITEM.get()) &&
                    !stack.is(Items.GUNPOWDER) &&
                    !stack.is(Items.TNT)) {
                cir.setReturnValue(InteractionResult.PASS);
            }
        }
    }
}