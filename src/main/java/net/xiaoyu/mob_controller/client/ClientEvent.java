package net.xiaoyu.mob_controller.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.xiaoyu.mob_controller.client.gui.GuiArmor;
import net.xiaoyu.mob_controller.registry.ModMenuType;
// 不再导入自定义渲染器

/**
 * 客户端模组事件处理器。
 *
 * <p>在客户端生命周期中注册菜单界面与实体渲染器。</p>
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvent {
    /**
     * 客户端初始化阶段注册菜单界面。
     *
     * @param event Forge 客户端初始化事件
     */
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenuType.ARMOR_MENU.get(), GuiArmor::new));
    }

    /**
     * 注册受控实体渲染器。
     *
     * @param event 实体渲染器注册事件
     */
    @SubscribeEvent
    public static void registerEntityRenders(EntityRenderersEvent.RegisterRenderers event) {
        // 已移除对 CONTROLLED_PILLAGER 和 CONTROLLED_WITCH 的渲染器注册
    }
}