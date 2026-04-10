package net.xiaoyu.mob_controller.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.xiaoyu.mob_controller.MobController;
/**
 * 本模组网络通道管理器，负责创建 {@link SimpleChannel} 并注册所有自定义数据包。
 *
 * <p>当前协议版本固定为 {@code 1}，客户端与服务端版本必须一致才允许连接。</p>
 */

public class NetWorkManager {
    /** 网络协议版本号。 */
    public static final String PROTOCOL_VERSION = "1";
    /** 本模组网络通道实例。 */
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MobController.MOD_ID, "control_mode_toggle"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    /**
     * 注册本模组全部网络数据包。
     *
     * <p>注册顺序决定包 ID，请保持稳定以避免协议不兼容。</p>
     */
    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(id++, ToggleControlModePacket.class, ToggleControlModePacket::toBytes,
                ToggleControlModePacket::new, ToggleControlModePacket::handle);
        INSTANCE.registerMessage(id++, MobControlCapabilitySyncPacket.class, MobControlCapabilitySyncPacket::toBytes,
                MobControlCapabilitySyncPacket::new, MobControlCapabilitySyncPacket::handle);
        INSTANCE.registerMessage(id++, ApplyControlCommandPacket.class, ApplyControlCommandPacket::toBytes,
            ApplyControlCommandPacket::new, ApplyControlCommandPacket::handle);
    }
}
