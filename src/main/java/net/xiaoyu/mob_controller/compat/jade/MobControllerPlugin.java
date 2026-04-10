package net.xiaoyu.mob_controller.compat.jade;

import net.minecraft.world.entity.Entity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade/WTHIT 兼容插件入口。
 */
@WailaPlugin
public class MobControllerPlugin implements IWailaPlugin {

    /**
     * 注册通用端（服务端）数据提供器。
     */
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(MobControllerProvider.INSTANCE, Entity.class);
    }

    /**
     * 注册客户端提示组件。
     */
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(MobControllerProvider.INSTANCE, Entity.class);
        /* registration.registerEntityComponent(MobControllerNameProvider.INSTANCE, Entity.class); */
    }
}
