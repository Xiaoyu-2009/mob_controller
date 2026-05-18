package net.xiaoyu.mob_controller.network;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.xiaoyu.mob_controller.network.client.ClientPacketHandler;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 服务端 → 客户端：同步某个玩家的队伍颜色。
 * 当玩家切换颜色时，此包会广播给所有在线玩家。
 */
public record SyncLegionColorPacket(UUID playerUUID, int colorRGB) {

    public SyncLegionColorPacket(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeInt(colorRGB);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientPacketHandler.handleSyncLegionColor(this));
        ctx.get().setPacketHandled(true);
    }
}