package net.xiaoyu.mob_controller.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.xiaoyu.mob_controller.client.gui.GuiArmor;
import net.xiaoyu.mob_controller.client.renderner.RendererControlledPillager;
import net.xiaoyu.mob_controller.client.renderner.RendererControlledWitch;
import net.xiaoyu.mob_controller.registry.ModEntities;
import net.xiaoyu.mob_controller.registry.ModMenuType;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientEvent {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenuType.ARMOR_MENU.get(), GuiArmor::new));
    }

    @SubscribeEvent
    public static void registerEntityRenders(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CONTROLLED_PILLAGER.get(), RendererControlledPillager::new);
        event.registerEntityRenderer(ModEntities.CONTROLLED_WITCH.get(), RendererControlledWitch::new);
    }
}