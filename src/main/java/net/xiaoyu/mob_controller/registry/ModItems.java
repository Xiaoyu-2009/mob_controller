package net.xiaoyu.mob_controller.registry;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.xiaoyu.mob_controller.MobController;
import net.xiaoyu.mob_controller.item.MobArmor;
import net.xiaoyu.mob_controller.item.MobControllerItem;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MobController.MOD_ID);

    // 生物控制器
    public static final RegistryObject<Item> MOB_CONTROLLER_ITEM = ITEMS.register("mob_controller",
            () -> new MobControllerItem(new Item.Properties().stacksTo(1)));

    // 盔甲编辑杖
    public static final RegistryObject<Item> ARMOR_EDITING_BLUEPRINT = ITEMS.register("armor_editing_blueprint",
            () -> new MobArmor(new Item.Properties().stacksTo(1)));
}