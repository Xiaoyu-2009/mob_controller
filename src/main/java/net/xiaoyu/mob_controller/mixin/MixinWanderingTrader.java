package net.xiaoyu.mob_controller.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WanderingTrader.class)
public abstract class MixinWanderingTrader {

    private static final String ORIGINAL_DESPAWN_DELAY_TAG = "mob_controller:original_despawn_delay";

    // 原有：潜行时阻止交易界面打开
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void onMobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        WanderingTrader trader = (WanderingTrader) (Object) this;
        if (MobControlledData.isControlledEntity(trader) &&
                MobControlledData.getControllerUUID(trader) != null &&
                MobControlledData.getControllerUUID(trader).equals(player.getUUID())) {
            if (player.isShiftKeyDown()) {
                cir.setReturnValue(InteractionResult.PASS);
            }
        }
    }

    // 阻止消失：受控时取消 maybeDespawn 并强制 despawnDelay = 0
    @Inject(method = "maybeDespawn", at = @At("HEAD"), cancellable = true)
    private void onMaybeDespawn(CallbackInfo ci) {
        WanderingTrader trader = (WanderingTrader) (Object) this;
        if (MobControlledData.isControlledEntity(trader)) {
            // 首次受控时保存原始 despawnDelay
            CompoundTag persistentData = trader.getPersistentData();
            if (!persistentData.contains(ORIGINAL_DESPAWN_DELAY_TAG)) {
                persistentData.putInt(ORIGINAL_DESPAWN_DELAY_TAG, trader.getDespawnDelay());
            }
            // 设置 despawnDelay = 0，确保永不消失
            if (trader.getDespawnDelay() != 0) {
                trader.setDespawnDelay(0);
            }
            ci.cancel(); // 阻止原 maybeDespawn 继续执行（原本会减少计数并可能 discard）
        } else {
            // 如果不再受控，且之前保存过原始值，则恢复
            CompoundTag persistentData = trader.getPersistentData();
            if (persistentData.contains(ORIGINAL_DESPAWN_DELAY_TAG)) {
                int original = persistentData.getInt(ORIGINAL_DESPAWN_DELAY_TAG);
                trader.setDespawnDelay(original);
                persistentData.remove(ORIGINAL_DESPAWN_DELAY_TAG);
            }
            // 非受控时不取消，让原逻辑正常执行（可能因为 despawnDelay 非零而消失）
        }
    }

    // 无限交易：交易成功后重置所有交易的使用次数为 0
    @Inject(method = "rewardTradeXp", at = @At("RETURN"))
    private void onRewardTradeXp(MerchantOffer offer, CallbackInfo ci) {
        WanderingTrader trader = (WanderingTrader) (Object) this;
        if (!MobControlledData.isControlledEntity(trader)) {
            return;
        }
        if (!Config.INFINITE_TRADES_FOR_CONTROLLED_WANDERING_TRADER.get()) return;
        MerchantOffers offers = trader.getOffers();
        if (offers != null && !offers.isEmpty()) {
            for (MerchantOffer o : offers) {
                ((MerchantOfferAccessor) o).setUses(0);
            }
        }
    }
}