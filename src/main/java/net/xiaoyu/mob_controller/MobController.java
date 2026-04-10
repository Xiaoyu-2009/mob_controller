package net.xiaoyu.mob_controller;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.xiaoyu.mob_controller.capability.MobControlCapabilityRegister;
import net.xiaoyu.mob_controller.event.MobControllerEvent;
import net.xiaoyu.mob_controller.network.NetWorkManager;
import net.xiaoyu.mob_controller.registry.ModEffects;
import net.xiaoyu.mob_controller.registry.ModEntities;
import net.xiaoyu.mob_controller.registry.ModItems;
import net.xiaoyu.mob_controller.registry.ModMenuType;
/**
 * Mob Controller 模组的主入口类，负责在 Forge 模组加载阶段完成所有子系统的注册与初始化。
 *
 * <p>该类在构造器中依次完成：
 * <ol>
 *   <li>物品、菜单、实体、药水效果的延迟注册器绑定到模组事件总线；</li>
 *   <li>创意模式物品栏注册；</li>
 *   <li>通用配置文件（{@link Config}）注册；</li>
 *   <li>游戏事件监听器（{@link net.xiaoyu.mob_controller.event.MobControllerEvent}）注册到 Forge 事件总线；</li>
 *   <li>能力（Capability）注册监听器绑定；</li>
 *   <li>网络通道注册。</li>
 * </ol>
 * </p>
 *
 * @see Config
 * @see net.xiaoyu.mob_controller.event.MobControllerEvent
 * @see net.xiaoyu.mob_controller.network.NetWorkManager
 */
@Mod(MobController.MOD_ID)
public class MobController {
    /** 模组 ID，与 {@code mods.toml} 以及资源路径保持一致。 */
    public static final String MOD_ID = "mob_controller";

    /**
     * 生成带有本模组命名空间的 {@link ResourceLocation}。
     *
     * @param s 资源路径（不含命名空间前缀）
     * @return 形如 {@code mob_controller:<s>} 的资源定位符
     */
    public static ResourceLocation prefix(String s) {
        return new ResourceLocation(MOD_ID, s);
    }

    /**
     * 模组构造器，由 Forge 在模组初始化阶段调用。
     *
     * <p>构造器内完成所有注册工作，执行顺序如下：</p>
     * <ol>
     *   <li>将各延迟注册器注册到模组事件总线；</li>
     *   <li>注册通用配置；</li>
     *   <li>向 MinecraftForge 事件总线注册主事件类；</li>
     *   <li>向模组事件总线注册能力注册类；</li>
     *   <li>初始化网络通道。</li>
     * </ol>
     */
    public MobController() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(eventBus);
        ModMenuType.MENU_TYPE.register(eventBus);
        ModEntities.ENTITIES.register(eventBus);
        ModEffects.MOB_EFFECTS.register(eventBus);
        ModEffects.POTIONS.register(eventBus);
        CreativeTab.register(eventBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        MinecraftForge.EVENT_BUS.register(MobControllerEvent.class);
        eventBus.register(MobControlCapabilityRegister.class);
        NetWorkManager.register();
    }
}
