package net.xiaoyu.mob_controller.event;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.RideSpeedConfigCache;

@Mod.EventBusSubscriber(modid = "mob_controller", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ConfigReloadListener {
    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getType() == ModConfig.Type.COMMON) {
            RideSpeedConfigCache.resetCache();
            MobControlUtil.resetAllRideableCache();
            MobControlUtil.resetCustomMaxCountsCache(); // 新增
        }
    }
}