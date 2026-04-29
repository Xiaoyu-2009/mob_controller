package net.xiaoyu.mob_controller.compat.jade;

import net.minecraft.world.entity.Entity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade/WTHIT 兼容插件入口。
 *
 * <p>注册服务端数据提供器、客户端信息提供器以及专门负责移除原版主人行的组件。</p>
 */
@WailaPlugin
public class MobControllerPlugin implements IWailaPlugin {

    /**
     * 注册通用端（服务端）数据提供器。
     *
     * @param registration 通用注册器
     */
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(MobControllerProvider.INSTANCE, Entity.class);
    }

    /**
     * 注册客户端提示组件。
     *
     * @param registration 客户端注册器
     */
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // 注册信息提供器（优先级0，快速添加控制者信息）
        registration.registerEntityComponent(MobControllerProvider.INSTANCE, Entity.class);
        // 注册专门的移除主人行组件（优先级10，在原版主人添加后清除）
        registration.registerEntityComponent(MobControllerOwnerRemover.INSTANCE, Entity.class);
    }
}