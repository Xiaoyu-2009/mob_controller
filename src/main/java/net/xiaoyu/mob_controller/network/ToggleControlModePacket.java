package net.xiaoyu.mob_controller.network;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.xiaoyu.mob_controller.MobController;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;

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

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();

            if (player != null && player.level().getEntity(this.entityId) instanceof Mob mob) {
                if (MobControlledData.isControlledMob(mob) && MobControlledData.getControllerUUID(mob).equals(player.getUUID())) {

                    MobControlledData.ControlMode newMode = MobControlledData.toggleControlMode(mob);

                    String mobName = mob.getDisplayName().getString();
                    String modeKey = "mob_controller.mode." + newMode.toString().toLowerCase();

                    MobControlUtil.showMessageToPlayer(player, mobName, modeKey, new Object[]{}, ChatFormatting.GOLD);
                }
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }

    public static final String PROTOCOL_VERSION = "1";
    public static SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MobController.MOD_ID, "control_mode_toggle"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        INSTANCE.registerMessage(
                0,
                ToggleControlModePacket.class,
                ToggleControlModePacket::toBytes,
                ToggleControlModePacket::new,
                ToggleControlModePacket::handle
        );
    }
}