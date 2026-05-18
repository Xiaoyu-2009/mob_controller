// 路径：net.xiaoyu.mob_controller.util.RideSpeedConfigCache
package net.xiaoyu.mob_controller.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.xiaoyu.mob_controller.Config;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 自定义骑乘速度配置缓存，避免每 tick 解析配置文件。
 */
public class RideSpeedConfigCache {
    private static final ConcurrentMap<String, Double> CUSTOM_SPEED_CACHE = new ConcurrentHashMap<>();
    private static volatile boolean cacheLoaded = false;

    private static void loadCache() {
        if (cacheLoaded) return;
        synchronized (RideSpeedConfigCache.class) {
            if (cacheLoaded) return;
            CUSTOM_SPEED_CACHE.clear();
            for (String entry : Config.RIDE_CUSTOM_SPEED.get()) {
                String[] parts = entry.split(",");
                if (parts.length == 2) {
                    try {
                        double speed = Double.parseDouble(parts[1]);
                        CUSTOM_SPEED_CACHE.put(parts[0], speed);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            cacheLoaded = true;
        }
    }

    public static void resetCache() {
        cacheLoaded = false;
        CUSTOM_SPEED_CACHE.clear();
    }

    public static boolean hasCustomRideSpeed(LivingEntity entity) {
        loadCache();
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key != null && CUSTOM_SPEED_CACHE.containsKey(key.toString());
    }

    public static double getCustomRideSpeed(LivingEntity entity) {
        loadCache();
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (key == null) return 0.35;
        return CUSTOM_SPEED_CACHE.getOrDefault(key.toString(), 0.35);
    }
}