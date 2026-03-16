package net.xiaoyu.mob_controller.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.xiaoyu.mob_controller.MobController;

public class NetWorkManager {
    public static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MobController.MOD_ID, "control_mode_toggle"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(id++, ToggleControlModePacket.class, ToggleControlModePacket::toBytes,
                ToggleControlModePacket::new, ToggleControlModePacket::handle);
        INSTANCE.registerMessage(id++, MobControlCapabilitySyncPacket.class, MobControlCapabilitySyncPacket::toBytes,
                MobControlCapabilitySyncPacket::new, MobControlCapabilitySyncPacket::handle);
    }
}
