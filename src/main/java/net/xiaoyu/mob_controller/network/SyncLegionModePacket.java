package net.xiaoyu.mob_controller.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.xiaoyu.mob_controller.network.client.ClientPacketHandler;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 服务端 → 客户端：同步某个玩家的军团模式（是否开启）。
 */
public record SyncLegionModePacket(UUID playerUUID, boolean legionMode) {

    public SyncLegionModePacket(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readBoolean());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeBoolean(legionMode);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientPacketHandler.handleSyncLegionMode(this));
        ctx.get().setPacketHandled(true);
    }
}