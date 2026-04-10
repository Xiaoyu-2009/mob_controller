package net.xiaoyu.mob_controller.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.xiaoyu.mob_controller.MobController;
import net.xiaoyu.mob_controller.inv.ContainerArmor;

/**
 * 本模组菜单类型注册表。
 */
public class ModMenuType {

    /**
     * 菜单类型延迟注册器。
     */
    public static final DeferredRegister<MenuType<?>> MENU_TYPE = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MobController.MOD_ID);

    /**
     * 受控生物装备编辑菜单类型。
     */
    public static final RegistryObject<MenuType<ContainerArmor>> ARMOR_MENU = MENU_TYPE.register(
        "armor_menu", () -> IForgeMenuType.create(ContainerArmor::new)
    );
}
