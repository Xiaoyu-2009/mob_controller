package net.xiaoyu.mob_controller.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.xiaoyu.mob_controller.MobController;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MobController.MOD_ID);

    public static final RegistryObject<SoundEvent> CONTROL_COMMAND_USE =
            SOUNDS.register("control_command_use",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MobController.MOD_ID, "control_command_use")));

    public static final RegistryObject<SoundEvent> AGGRESSIVE_SWITCH_BATCH =
            SOUNDS.register("aggressive_switch_batch",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MobController.MOD_ID, "aggressive_switch_batch")));
}