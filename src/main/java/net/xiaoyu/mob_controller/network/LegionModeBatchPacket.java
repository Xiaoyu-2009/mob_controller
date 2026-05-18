package net.xiaoyu.mob_controller.network;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.xiaoyu.mob_controller.item.LegionBannerItem;
import net.xiaoyu.mob_controller.util.MobControlledData;

import java.util.function.Supplier;

public record LegionModeBatchPacket(boolean enable) {
    public LegionModeBatchPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(enable);
    }
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.getMainHandItem().getItem() instanceof LegionBannerItem) {
                int count = MobControlledData.setLegionModeForAll(player, 32, enable);
                String key = enable ? "mob_controller.message.legion_enable_batch" : "mob_controller.message.legion_disable_batch";
                player.displayClientMessage(Component.translatable(key, count).withStyle(ChatFormatting.GOLD), true);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}