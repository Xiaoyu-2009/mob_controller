package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.xiaoyu.mob_controller.item.MobControllerItem;
import net.xiaoyu.mob_controller.network.NetWorkManager;
import net.xiaoyu.mob_controller.network.ToggleControlModePacket;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

/**
 * 玩家交互行为注入。
 *
 * <p>将原版交互流程桥接到生物控制器物品逻辑。</p>
 */
@Mixin(Player.class)
abstract class PlayerMixin extends Entity {
    public PlayerMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * 注入 {@code interactOn} 头部：当手持生物控制器且目标是生物时，优先走控制逻辑。
     */
    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void onInteractOn(Entity entityToInteractOn, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = (Player) (Object) this;
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof MobControllerItem controllerItem) || !(entityToInteractOn instanceof Mob)) return;
        InteractionResult result = controllerItem.interactLivingEntity(stack, player, (LivingEntity) entityToInteractOn, hand);
        if (result != InteractionResult.PASS) cir.setReturnValue(result);
    }

    @Inject(method = "interactOn", at = @At("RETURN"))
    private void onInteractEnd(Entity entityToInteractOn, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(entityToInteractOn instanceof Mob mob)) return;
        InteractionResult returnValue = cir.getReturnValue();
        if (returnValue != InteractionResult.PASS && returnValue != InteractionResult.FAIL) return;
        Player player = (Player) (Object) this;
        // 手持生物控制器时不允许切换模式（控制器仅用于驯服，不负责指令）
        if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof MobControllerItem) return;
        if (player.getItemInHand(hand).getItem() instanceof MobControllerItem) return;
        if (
            !MobControlledData.isControlledEntity(mob)
            || !Objects.equals(
                MobControlledData.getControllerUUID(mob),
                this.getUUID()
            )
        ) {
            return;
        }
        if (MobControlUtil.isDirectRideableControlledMob(mob) && !player.isShiftKeyDown()) {
            return;
        }
        NetWorkManager.INSTANCE.sendToServer(new ToggleControlModePacket(mob.getId()));
    }
}
