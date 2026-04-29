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
// 不再导入自定义实体类

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

    // 已移除 CONTROLLED_PILLAGER 和 CONTROLLED_WITCH 的注册

    /**
     * 注册受控实体属性。
     *
     * @param event 实体属性创建事件
     */
    @SubscribeEvent
    public static void onEntityAttributeCreationEvent(EntityAttributeCreationEvent event) {
        // 已移除对自定义实体的属性注册
    }
}