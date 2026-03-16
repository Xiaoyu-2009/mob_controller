package net.xiaoyu.mob_controller.network;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.NetworkEvent;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;

import java.util.Objects;
import java.util.function.Supplier;

public class ToggleControlModePacket {
    private final int entityId;

    public ToggleControlModePacket(int entityId) {
        this.entityId = entityId;
    }

    public ToggleControlModePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();

            if (player != null && player.level().getEntity(this.entityId) instanceof Mob mob) {
                if (MobControlledData.isControlledEntity(mob) && Objects.equals(MobControlledData.getControllerUUID(mob), player.getUUID())) {

                    MobControlledData.ControlMode newMode = MobControlledData.toggleControlMode(mob);

                    String mobName = mob.getDisplayName().getString();
                    String modeKey = "mob_controller.mode." + newMode.toString().toLowerCase();

                    MobControlUtil.showMessageToPlayer(player, mobName, modeKey, new Object[]{}, ChatFormatting.GOLD);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}