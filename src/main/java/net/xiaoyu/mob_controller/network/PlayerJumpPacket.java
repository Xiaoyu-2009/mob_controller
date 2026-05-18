package net.xiaoyu.mob_controller.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PlayerJumpPacket(boolean isJumping) {

    public PlayerJumpPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(isJumping);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                player.getPersistentData().putBoolean("mob_controller:jump_pressed", isJumping);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}