package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 访问 MerchantOffer 的 uses 字段，用于重置交易次数。
 */
@Mixin(MerchantOffer.class)
public interface MerchantOfferAccessor {
    @Accessor("uses")
    void setUses(int uses);
}