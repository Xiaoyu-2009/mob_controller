package net.xiaoyu.mob_controller.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.*;
import net.xiaoyu.mob_controller.MobController;
import net.xiaoyu.mob_controller.inv.ContainerArmor;

public class ModMenuType {

    public static final DeferredRegister<MenuType<?>> MENU_TYPE = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MobController.MOD_ID);

    public static final RegistryObject<MenuType<ContainerArmor>> ARMOR_MENU = MENU_TYPE.register(
        "armor_menu", () -> IForgeMenuType.create(ContainerArmor::new)
    );
}