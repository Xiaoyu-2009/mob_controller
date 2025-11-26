package net.xiaoyu.mob_controller;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import net.xiaoyu.mob_controller.item.ModItems;

public class CreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MobController.MOD_ID);

    // 生物控制器
    public static final RegistryObject<CreativeModeTab> MOB_CONTROLLER_TAB = CREATIVE_MODE_TABS.register("mob_controller_tab",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.mob_controller"))
            .icon(() -> new ItemStack(ModItems.MOB_CONTROLLER_ITEM.get()))
            .displayItems((params, output) -> {
                // 生物控制器
                output.accept(ModItems.MOB_CONTROLLER_ITEM.get());
                // 盔甲编辑杖
                output.accept(ModItems.ARMOR_EDITING_STAFF.get());
            })
            .build());
            
    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}