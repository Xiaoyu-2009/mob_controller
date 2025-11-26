package net.xiaoyu.mob_controller.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.xiaoyu.mob_controller.client.gui.GuiArmor;
import net.xiaoyu.mob_controller.registry.ModMenuType;

public class ClientEvent {
    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ClientEvent());
        IEventBus modbus = FMLJavaModLoadingContext.get().getModEventBus();
        modbus.addListener(ClientEvent::clientSetup);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenuType.ARMOR_MENU.get(), GuiArmor::new));
    }
}