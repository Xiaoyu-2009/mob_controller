package net.xiaoyu.mob_controller.network;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.xiaoyu.mob_controller.item.MobControllerItem;
import net.xiaoyu.mob_controller.registry.ModItems;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;

import java.util.function.Supplier;

public class ApplyControlCommandPacket {
    private final MobControlledData.ControlMode mode;

    public ApplyControlCommandPacket(MobControlledData.ControlMode mode) {
        this.mode = mode;
    }

    public ApplyControlCommandPacket(FriendlyByteBuf buf) {
        this.mode = MobControlledData.ControlMode.values()[buf.readVarInt()];
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.mode.ordinal());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !player.getMainHandItem().is(ModItems.MOB_CONTROLLER_ITEM.get())) {
                return;
            }

            int affectedCount = MobControllerItem.applyControlCommand(player, this.mode);
            String modeKey = "mob_controller.mode." + this.mode.toString().toLowerCase();
            MobControlUtil.showMessageToPlayer(player, "[" + affectedCount + "]", modeKey, new Object[]{}, ChatFormatting.GOLD);
        });
        ctx.get().setPacketHandled(true);
    }
}

