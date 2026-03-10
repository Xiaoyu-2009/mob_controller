package net.xiaoyu.mob_controller;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.xiaoyu.mob_controller.capability.MobControlCapabilityRegister;
import net.xiaoyu.mob_controller.event.MobControllerEvent;
import net.xiaoyu.mob_controller.network.ToggleControlModePacket;
import net.xiaoyu.mob_controller.registry.ModEffects;
import net.xiaoyu.mob_controller.registry.ModEntities;
import net.xiaoyu.mob_controller.registry.ModItems;
import net.xiaoyu.mob_controller.registry.ModMenuType;

@Mod(MobController.MOD_ID)
public class MobController {
    public static final String MOD_ID = "mob_controller";

    public static ResourceLocation prefix(String s) {
        return new ResourceLocation(MOD_ID, s);
    }

    public MobController() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(eventBus);
        ModMenuType.MENU_TYPE.register(eventBus);
        ModEntities.ENTITIES.register(eventBus);
        ModEffects.MOB_EFFECTS.register(eventBus);
        ModEffects.POTIONS.register(eventBus);
        CreativeTab.register(eventBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        MinecraftForge.EVENT_BUS.register(MobControllerEvent.class);
        eventBus.register(MobControlCapabilityRegister.class);
        ToggleControlModePacket.register();
    }
}