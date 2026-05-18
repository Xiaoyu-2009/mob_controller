package net.xiaoyu.mob_controller.network;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.xiaoyu.mob_controller.item.LegionBannerItem;
import net.xiaoyu.mob_controller.util.MobControlledData;

import java.util.function.Supplier;

public record UpdateLegionColorPacket(int delta) {

    public UpdateLegionColorPacket(FriendlyByteBuf buf) {
        this(buf.readInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(delta);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !(player.getMainHandItem().getItem() instanceof LegionBannerItem)) return;

            // 1. 切换队伍颜色
            LegionBannerItem.cycleColor(player, delta);

            // 2. 获取新颜色的 RGB
            ChatFormatting newColor = LegionBannerItem.getLegionColor(player);
            int newRgb = LegionBannerItem.getColorRGB(newColor);   // 注意：此方法需返回 ARGB (0xFFRRGGBB)

            // 3. 发包给客户端所有玩家
            NetWorkManager.INSTANCE.send(PacketDistributor.ALL.noArg(),
                    new SyncLegionColorPacket(player.getUUID(), newRgb));

            // 4. message
            Component teamName = LegionBannerItem.getTeamDisplayName(newColor);
            player.displayClientMessage(
                    Component.translatable("mob_controller.message.legion_color", teamName)
                            .withStyle(ChatFormatting.AQUA),
                    true
            );

            // 5. 清理因颜色变更不再敌对的军团战斗目标（保持原有逻辑）
            MobControlledData.clearLegionTargetsAfterPlayerColorChange(player);
        });
        ctx.get().setPacketHandled(true);
    }
}