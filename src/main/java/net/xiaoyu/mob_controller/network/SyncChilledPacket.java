package net.xiaoyu.mob_controller.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.xiaoyu.mob_controller.network.client.ClientPacketHandler;

import java.util.function.Supplier;

public record SyncChilledPacket(int entityId, boolean chilled) {

    public SyncChilledPacket(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readBoolean());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(chilled);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientPacketHandler.handleSyncChilled(this));
        ctx.get().setPacketHandled(true);
    }
}