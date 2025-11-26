package net.xiaoyu.mob_controller.capability;

import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class MobControlCapabilityRegister {
    public static final Capability<MobControlCapability> MOB_CONTROL_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(MobControlCapability.class);

        MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY = MOB_CONTROL_CAPABILITY;
    }
}