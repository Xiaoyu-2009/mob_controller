// 文件：net/xiaoyu/mob_controller/network/SyncSelectedModePacket.java
package net.xiaoyu.mob_controller.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.xiaoyu.mob_controller.item.ModeSelectControlCommandItem;
import net.xiaoyu.mob_controller.util.MobControlledData;

import java.util.function.Supplier;

public record SyncSelectedModePacket(MobControlledData.ControlMode mode) {
    public SyncSelectedModePacket(FriendlyByteBuf buf) {
        this(MobControlledData.ControlMode.values()[buf.readVarInt()]);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(mode.ordinal());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ItemStack mainHand = player.getMainHandItem();
                if (mainHand.getItem() instanceof ModeSelectControlCommandItem) {
                    ModeSelectControlCommandItem.setSelectedMode(mainHand, mode);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}