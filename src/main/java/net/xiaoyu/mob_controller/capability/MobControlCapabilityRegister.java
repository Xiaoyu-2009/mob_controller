package net.xiaoyu.mob_controller.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 在模组事件总线上监听 {@link RegisterCapabilitiesEvent}，完成 {@link MobControlCapability} 的注册，
 * 并将全局 Capability 令牌同步给 {@link MobControlCapabilityProvider}。
 *
 * <p>该类标注了 {@link Mod.EventBusSubscriber}，Forge 会在模组加载阶段自动注册其中的事件处理方法。</p>
 *
 * @see MobControlCapability
 * @see MobControlCapabilityProvider
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class MobControlCapabilityRegister {
    /**
     * 本模组的 Capability 类型令牌，通过 {@link CapabilityManager#get(CapabilityToken)} 获取。
     *
     * <p>在 {@link #registerCapabilities(RegisterCapabilitiesEvent)} 被调用之前，
     * 该令牌处于未初始化状态，不应提前使用。</p>
     */
    public static final Capability<MobControlCapability> MOB_CONTROL_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });

    /**
     * 在 Forge 的 {@link RegisterCapabilitiesEvent} 中注册 {@link MobControlCapability} 类型，
     * 并将令牌赋值给 {@link MobControlCapabilityProvider#MOB_CONTROL_CAPABILITY}，
     * 使提供者能够通过令牌响应 {@code getCapability} 调用。
     *
     * @param event Forge 能力注册事件，由模组事件总线分发
     */
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(MobControlCapability.class);
        event.register(WaxedCapability.class);
        event.register(ChilledCapability.class);
        MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY = MOB_CONTROL_CAPABILITY;
    }
}
