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

/**
 * 本模组实体类型注册表。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntities {
    /**
     * 实体类型延迟注册器。
     */
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(
        ForgeRegistries.ENTITY_TYPES,
        MobController.MOD_ID
    );

    /**
     * 受控掠夺者实体类型。
     */
    public static final RegistryObject<EntityType<EntityControlledPillager>> CONTROLLED_PILLAGER = ENTITIES.register(
        "controlled_pillager", () ->
            EntityType.Builder.of(EntityControlledPillager::new, MobCategory.MONSTER)
                .sized(0.6F, 1.95F).clientTrackingRange(8)
                .build(MobController.location("controlled_pillager").toString())
    );
    /**
     * 受控女巫实体类型。
     */
    public static final RegistryObject<EntityType<EntityControlledWitch>> CONTROLLED_WITCH = ENTITIES.register(
        "controlled_witch", () ->
            EntityType.Builder.of(EntityControlledWitch::new, MobCategory.MONSTER)
                .sized(0.6F, 1.95F).clientTrackingRange(8)
                .build(MobController.location("controlled_witch").toString())
    );

    /**
     * 注册受控实体属性。
     *
     * @param event 实体属性创建事件
     */
    @SubscribeEvent
    public static void onEntityAttributeCreationEvent(EntityAttributeCreationEvent event) {
        event.put(CONTROLLED_PILLAGER.get(), EntityControlledPillager.createAttributes().build());
        event.put(CONTROLLED_WITCH.get(), EntityControlledWitch.createAttributes().build());
    }
}
