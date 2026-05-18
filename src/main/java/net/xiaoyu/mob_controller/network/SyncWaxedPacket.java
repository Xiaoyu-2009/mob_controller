package net.xiaoyu.mob_controller.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.xiaoyu.mob_controller.network.client.ClientPacketHandler;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：同步某个实体的打蜡状态。
 */
public record SyncWaxedPacket(int entityId, boolean waxed) {

    public SyncWaxedPacket(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readBoolean());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(waxed);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientPacketHandler.handleSyncWaxed(this));
        ctx.get().setPacketHandled(true);
    }
}