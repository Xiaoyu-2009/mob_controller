package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.*;
import net.xiaoyu.mob_controller.item.MobControllerItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {
    
    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void onInteractOn(Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = (Player) (Object) this;
        ItemStack stack = player.getItemInHand(hand);

        if (stack.getItem() instanceof MobControllerItem && entity instanceof Mob) {
            MobControllerItem controllerItem = (MobControllerItem) stack.getItem();
            InteractionResult result = controllerItem.interactLivingEntity(stack, player, (LivingEntity) entity, hand);
            
            if (result != InteractionResult.PASS) {
                cir.setReturnValue(result);
            }
        }
    }
}