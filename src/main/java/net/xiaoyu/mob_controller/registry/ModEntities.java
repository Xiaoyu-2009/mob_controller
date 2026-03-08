package net.xiaoyu.mob_controller.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.xiaoyu.mob_controller.MobController;
import net.xiaoyu.mob_controller.entity.EntityControlledPillager;
import net.xiaoyu.mob_controller.entity.EntityControlledWitch;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MobController.MOD_ID);

    /**
     * TODO: 尚未本地化
     */
    public static final RegistryObject<EntityType<EntityControlledPillager>> CONTROLLED_PILLAGER = ENTITIES.register("controlled_pillager", () ->
            EntityType.Builder.of(EntityControlledPillager::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(8)
                    .build(MobController.prefix("controlled_pillager").toString()));
    public static final RegistryObject<EntityType<EntityControlledWitch>> CONTROLLED_WITCH = ENTITIES.register("controlled_witch", () ->
            EntityType.Builder.of(EntityControlledWitch::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(8)
                    .build(MobController.prefix("controlled_witch").toString()));

    @SubscribeEvent
    public static void onEntityAttributeCreationEvent(EntityAttributeCreationEvent event) {
        event.put(CONTROLLED_PILLAGER.get(), EntityControlledPillager.createAttributes().build());
        event.put(CONTROLLED_WITCH.get(), EntityControlledWitch.createAttributes().build());
    }
}
