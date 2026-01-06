package net.xiaoyu.mob_controller;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.xiaoyu.mob_controller.event.MobControllerEvent;
import net.xiaoyu.mob_controller.item.ModItems;
import net.xiaoyu.mob_controller.capability.MobControlCapabilityRegister;
import net.xiaoyu.mob_controller.registry.ModMenuType;
import net.xiaoyu.mob_controller.client.ClientEvent;
import net.xiaoyu.mob_controller.network.ToggleControlModePacket;

@Mod(MobController.MOD_ID)
public class MobController {
    public static final String MOD_ID = "mob_controller";

    public MobController() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(eventBus);
        ModMenuType.MENU_TYPE.register(eventBus);
        CreativeTab.register(eventBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(MobControllerEvent.class);
        eventBus.register(MobControlCapabilityRegister.class);
        ClientEvent.register();
        ToggleControlModePacket.register();
    }
}