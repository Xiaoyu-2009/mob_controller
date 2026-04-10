package net.xiaoyu.mob_controller;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.xiaoyu.mob_controller.registry.ModItems;

/**
 * 注册并管理本模组在创造模式物品栏中的独立创造模式标签页。
 *
 * <p>标签页标识为 {@code mob_controller:mob_controller_tab}，
 * 显示名称通过翻译键 {@code itemGroup.mob_controller} 本地化，
 * 图标为生物控制器物品（{@link ModItems#MOB_CONTROLLER_ITEM}）。</p>
 *
 * <p>标签页内包含以下物品（按展示顺序）：</p>
 * <ol>
 *   <li>生物控制器 —— 用于控制生物；</li>
 *   <li>控制令 —— 批量发布控制指令；</li>
 *   <li>心变契约 —— 解除对生物的控制；</li>
 *   <li>盔甲编辑蓝图 —— 打开被控生物的装备界面。</li>
 * </ol>
 */
public class CreativeTab {
    /**
     * 创造模式标签页的延迟注册器，命名空间与模组 ID 保持一致。
     *
     * @see MobController#MOD_ID
     */
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MobController.MOD_ID);

    /**
     * 本模组在创造模式物品栏中的标签页注册对象。
     *
     * <p>懒加载，在 Forge 注册阶段通过 {@link #CREATIVE_MODE_TABS} 完成实例化。</p>
     */
    public static final RegistryObject<CreativeModeTab> MOB_CONTROLLER_TAB = CREATIVE_MODE_TABS.register("mob_controller_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mob_controller"))
                    .icon(() -> new ItemStack(ModItems.MOB_CONTROLLER_ITEM.get()))
                    .displayItems((params, output) -> {
                        // 生物控制器
                        output.accept(ModItems.MOB_CONTROLLER_ITEM.get());
                        // 控制令
                        output.accept(ModItems.CONTROL_COMMAND_ITEM.get());
                        // 心变契约
                        output.accept(ModItems.HEART_CONTRACT_ITEM.get());
                        // 盔甲编辑蓝图
                        output.accept(ModItems.ARMOR_EDITING_BLUEPRINT.get());
                    })
                    .build());

    /**
     * 将 {@link #CREATIVE_MODE_TABS} 延迟注册器绑定到给定的模组事件总线，
     * 使其在适当的生命周期阶段完成注册。
     *
     * <p>应当在 {@link MobController} 构造器中调用此方法，且仅调用一次。</p>
     *
     * @param eventBus 模组专属的 Forge 事件总线，通过
     *                 {@link net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext#getModEventBus()} 获取
     */
    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
