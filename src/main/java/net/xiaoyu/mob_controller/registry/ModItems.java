// ============================================================
// 源文件: C:/Users/Mnibr/Desktop/生物控制器源码/mob_controller-1.20.1-Forge/src\main\java\net\xiaoyu\mob_controller\registry\ModItems.java
// ============================================================

package net.xiaoyu.mob_controller.registry;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.xiaoyu.mob_controller.MobController;
import net.xiaoyu.mob_controller.item.*;

/**
 * 本模组物品注册表。
 */
public class ModItems {
    /**
     * 物品延迟注册器。
     */
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MobController.MOD_ID);

    /**
     * 生物控制器。
     */
    public static final RegistryObject<Item> MOB_CONTROLLER_ITEM = ITEMS.register(
            "mob_controller",
            () -> new MobControllerItem(new Item.Properties().stacksTo(1))
    );

    /**
     * 控制令。
     */
    public static final RegistryObject<Item> CONTROL_COMMAND_ITEM = ITEMS.register(
            "control_command",
            () -> new ControlCommandItem(new Item.Properties().stacksTo(1))
    );

    /**
     * 心变契约。
     */
    public static final RegistryObject<Item> HEART_CONTRACT_ITEM = ITEMS.register(
            "heart_contract",
            () -> new HeartContractItem(new Item.Properties().stacksTo(1))
    );

    /**
     * 盔甲编辑蓝图。
     */
    public static final RegistryObject<Item> ARMOR_EDITING_BLUEPRINT = ITEMS.register(
            "armor_editing_blueprint",
            () -> new MobArmor(new Item.Properties().stacksTo(1))
    );

    /**
     * 护主切换器
     */
    public static final RegistryObject<Item> AGGRESSIVE_SWITCH_ITEM = ITEMS.register(
            "aggressive_switch",
            () -> new AggressiveSwitchItem(new Item.Properties().stacksTo(1))
    );


    /**
     * 骑乘令
     */
    public static final RegistryObject<Item> RIDE_COMMAND_ITEM = ITEMS.register(
            "ride_command",
            () -> new RideCommandItem(new Item.Properties().stacksTo(1))
    );

    /**
     * 创造模式生物控制器
     */
    public static final RegistryObject<Item> CREATIVE_MOB_CONTROLLER_ITEM = ITEMS.register(
            "creative_mob_controller",
            () -> new CreativeMobControllerItem(new Item.Properties().stacksTo(1))
    );

    /**
     * 五谷杂粮
     */
    public static final RegistryObject<Item> GRAIN_ITEM = ITEMS.register(
            "grain",
            () -> new GrainItem(new Item.Properties().stacksTo(64))
    );

    /**
     * 控制令切换模式版
     */
    public static final RegistryObject<Item> MODE_SELECT_CONTROL_COMMAND_ITEM = ITEMS.register(
            "mode_select_control_command",
            () -> new ModeSelectControlCommandItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> LEGION_BANNER_ITEM = ITEMS.register(
            "legion_banner",
            () -> new LegionBannerItem(new Item.Properties().stacksTo(1))
    );
}