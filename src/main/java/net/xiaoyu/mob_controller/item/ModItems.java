package net.xiaoyu.mob_controller.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.*;
import net.xiaoyu.mob_controller.MobController;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MobController.MOD_ID);
    
    // 生物控制器
    public static final RegistryObject<Item> MOB_CONTROLLER_ITEM = ITEMS.register("mob_controller", 
        () -> new MobControllerItem(new Item.Properties().stacksTo(1)));
        
    // 盔甲编辑杖
    public static final RegistryObject<Item> ARMOR_EDITING_BLUEPRINT = ITEMS.register("armor_editing_blueprint", 
        () -> new MobArmor(new Item.Properties().stacksTo(1)));
}