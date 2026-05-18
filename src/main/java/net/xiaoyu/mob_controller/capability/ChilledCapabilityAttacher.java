package net.xiaoyu.mob_controller.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xiaoyu.mob_controller.MobController;

@Mod.EventBusSubscriber(modid = MobController.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChilledCapabilityAttacher {

    private static final ResourceLocation CAPABILITY_KEY = new ResourceLocation(MobController.MOD_ID, "chilled");

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof SnowGolem) {
            event.addCapability(CAPABILITY_KEY, new ChilledCapabilityProvider());
        }
    }
}