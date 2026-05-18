package net.xiaoyu.mob_controller.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.xiaoyu.mob_controller.item.LegionBannerItem;

import java.util.function.Supplier;

/**
 * 客户端 -> 服务端：请求切换玩家自身的军团模式。
 */
public record TogglePlayerLegionModePacket() {

    public TogglePlayerLegionModePacket(FriendlyByteBuf buf) {
        this();
    }

    public void toBytes(FriendlyByteBuf buf) {
        // 无数据
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.getMainHandItem().getItem() instanceof LegionBannerItem) {
                LegionBannerItem.togglePlayerLegionMode(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}