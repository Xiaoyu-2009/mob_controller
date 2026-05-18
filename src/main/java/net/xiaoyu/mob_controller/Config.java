package net.xiaoyu.mob_controller;

import net.minecraftforge.common.ForgeConfigSpec;
import net.xiaoyu.mob_controller.config.*;

import java.util.List;

/**
 * 模组配置门面类，向后兼容原有 Config.XXX 静态字段。
 * 所有配置值实际由分类配置类提供。
 */
public class Config {
    // ========== 通用控制配置 ==========
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CUSTOM_CONTROL_RULES = ControlConfig.CUSTOM_CONTROL_RULES;

    // ========== 传送配置 ==========
    public static final ForgeConfigSpec.DoubleValue FOLLOW_TELEPORT_HORIZONTAL_DISTANCE = TeleportConfig.FOLLOW_TELEPORT_HORIZONTAL_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue FOLLOW_TELEPORT_VERTICAL_DISTANCE = TeleportConfig.FOLLOW_TELEPORT_VERTICAL_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue FOLLOW_MOVE_TO_DISTANCE = TeleportConfig.FOLLOW_MOVE_TO_DISTANCE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WATER_BASED_MOBS = TeleportConfig.WATER_BASED_MOBS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> LAND_BASED_MOBS = TeleportConfig.LAND_BASED_MOBS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> AMPHIBIAN_MOBS = TeleportConfig.AMPHIBIAN_MOBS;

    // ========== 功能配置 ==========
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_MOBS = FeatureConfig.BLACKLISTED_MOBS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> IGNORE_OWNER_TAG_MOBS = FeatureConfig.IGNORE_OWNER_TAG_MOBS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> STAY_WELDED_SPECIAL_AI_MOBS = FeatureConfig.STAY_WELDED_SPECIAL_AI_MOBS;
    public static final ForgeConfigSpec.BooleanValue ALWAYS_SUCCESS = FeatureConfig.ALWAYS_SUCCESS;
    public static final ForgeConfigSpec.BooleanValue CONTROLLED_MOBS_ATTACK_PLAYERS_ON_COMMAND = FeatureConfig.CONTROLLED_MOBS_ATTACK_PLAYERS_ON_COMMAND;
    public static final ForgeConfigSpec.IntValue CONTROLLED_MOB_HEAL_OUT_OF_COMBAT_DELAY_TICKS = FeatureConfig.CONTROLLED_MOB_HEAL_OUT_OF_COMBAT_DELAY_TICKS;
    public static final ForgeConfigSpec.IntValue HIGH_HEALTH_THRESHOLD = FeatureConfig.HIGH_HEALTH_THRESHOLD;
    public static final ForgeConfigSpec.IntValue RESPAWN_DELAY_TICKS = FeatureConfig.RESPAWN_DELAY_TICKS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_RESPAWN = FeatureConfig.ENABLE_RESPAWN;
    public static final ForgeConfigSpec.IntValue ATTACK_LIMIT = FeatureConfig.ATTACK_LIMIT;
    public static final ForgeConfigSpec.IntValue HEALTH_LIMIT = FeatureConfig.HEALTH_LIMIT;
    public static final ForgeConfigSpec.DoubleValue HEALTH_PERCENT_THRESHOLD = FeatureConfig.HEALTH_PERCENT_THRESHOLD;
    public static final ForgeConfigSpec.IntValue REQUIRED_HEALTH = FeatureConfig.REQUIRED_HEALTH;
    public static final ForgeConfigSpec.BooleanValue INFINITE_TRADES_FOR_CONTROLLED_WANDERING_TRADER = FeatureConfig.INFINITE_TRADES_FOR_CONTROLLED_WANDERING_TRADER;

    // ========== 物品配置 ==========
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MAX_RIDERS_PER_MOUNT = ItemConfig.MAX_RIDERS_PER_MOUNT;
    public static final ForgeConfigSpec.BooleanValue USE_GRAIN_RECIPE = ItemConfig.USE_GRAIN_RECIPE;

    // ========== 骑乘配置 ==========
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> LAND_RIDEABLE_MOBS = RidingConfig.LAND_RIDEABLE_MOBS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> AQUATIC_RIDEABLE_MOBS = RidingConfig.AQUATIC_RIDEABLE_MOBS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FLYING_RIDEABLE_MOBS = RidingConfig.FLYING_RIDEABLE_MOBS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RIDE_AMPHIBIAN_MOBS = RidingConfig.RIDE_AMPHIBIAN_MOBS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RIDE_FLYING_LAND_MOBS = RidingConfig.RIDE_FLYING_LAND_MOBS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RIDE_CUSTOM_SPEED = RidingConfig.RIDE_CUSTOM_SPEED;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RIDE_POSITION_Y_OFFSET = RidingConfig.RIDE_POSITION_Y_OFFSET;
    public static final ForgeConfigSpec.BooleanValue PREVENT_PLAYER_ATTACK_OWN_MOUNT = RidingConfig.PREVENT_PLAYER_ATTACK_OWN_MOUNT;
}