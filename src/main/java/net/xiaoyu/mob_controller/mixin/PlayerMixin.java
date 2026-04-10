package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.xiaoyu.mob_controller.item.MobControllerItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 玩家交互行为注入。
 *
 * <p>将原版交互流程桥接到生物控制器物品逻辑。</p>
 */
@Mixin(Player.class)
public class PlayerMixin {

    /**
     * 注入 {@code interactOn} 头部：当手持生物控制器且目标是生物时，优先走控制逻辑。
     */
    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void onInteractOn(Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = (Player) (Object) this;
        ItemStack stack = player.getItemInHand(hand);

        if (stack.getItem() instanceof MobControllerItem controllerItem && entity instanceof Mob) {
            InteractionResult result = controllerItem.interactLivingEntity(stack, player, (LivingEntity) entity, hand);

            if (result != InteractionResult.PASS) {
                cir.setReturnValue(result);
            }
        }
    }
}
