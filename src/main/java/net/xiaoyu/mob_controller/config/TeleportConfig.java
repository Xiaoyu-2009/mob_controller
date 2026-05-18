package net.xiaoyu.mob_controller.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.Arrays;
import java.util.List;

/**
 * 传送与跟随行为配置。
 */
public class TeleportConfig {
    public static final ForgeConfigSpec SPEC;

    /**
     * 水平传送触发距离（单位：格）。
     * 当受控生物与控制者的水平距离超过此值时，强制传送回到控制者身边。
     * 默认值：14.0。
     */
    public static final ForgeConfigSpec.DoubleValue FOLLOW_TELEPORT_HORIZONTAL_DISTANCE;

    /**
     * 垂直传送触发距离（单位：格）。
     * 当受控生物与控制者的垂直距离（绝对值）超过此值时，强制传送回到控制者身边。
     * 默认值：32.0。
     */
    public static final ForgeConfigSpec.DoubleValue FOLLOW_TELEPORT_VERTICAL_DISTANCE;

    /**
     * 跟随移动触发距离（单位：格）。
     * 当受控生物与控制者的距离超过此值时，生物会主动向控制者移动（而非原地待命）。
     * 默认值：8.0。
     */
    public static final ForgeConfigSpec.DoubleValue FOLLOW_MOVE_TO_DISTANCE;

    /**
     * 强制视为水生生物的列表（即使其原版类型不是 WATER）。
     * 列表中每一项为实体注册名，格式 "namespace:path"。
     * 当生物在此列表中时，传送落点一律选择水中。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WATER_BASED_MOBS;

    /**
     * 强制视为陆生生物的列表（即使其原版类型是 WATER 或在两栖列表中）。
     * 列表中每一项为实体注册名，格式 "namespace:path"。
     * 当生物在此列表中时，传送落点一律选择陆地（空气方块）。
     * 注意：此配置的优先级高于 water_based_mobs 和原版类型，但低于 amphibian_mobs 的特殊逻辑。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> LAND_BASED_MOBS;

    /**
     * 两栖动物生物列表（用于跟随传送时的落点决策：在水中优先）。
     * 当控制者完全浸没在水中时，这些生物会被传送到水中安全位置。
     * 默认包含海龟（turtle）和青蛙（frog）。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> AMPHIBIAN_MOBS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("===================================================");
        builder.comment("  Teleportation & Following");
        builder.comment("===================================================");
        builder.comment("");

        FOLLOW_TELEPORT_HORIZONTAL_DISTANCE = builder
                .comment("Horizontal distance (blocks) that triggers teleportation when following. Default: 14.0")
                .defineInRange("follow_teleport_horizontal_distance", 14.0, 1.0, 256.0);
        builder.comment("");

        FOLLOW_TELEPORT_VERTICAL_DISTANCE = builder
                .comment("Vertical distance (blocks) that triggers teleportation when following. Default: 32.0")
                .defineInRange("follow_teleport_vertical_distance", 32.0, 1.0, 256.0);
        builder.comment("");

        FOLLOW_MOVE_TO_DISTANCE = builder
                .comment("Distance (blocks) at which a controlled mob starts moving toward the controller when following. Default: 8.0")
                .defineInRange("follow_move_to_distance", 8.0, 1.0, 64.0);
        builder.comment("");

        WATER_BASED_MOBS = builder
                .comment("List of mobs that should always be treated as water-based for teleportation (overrides default type check).")
                .defineList("water_based_mobs", List.of("alexsmobs:skelewag"), obj -> obj instanceof String);
        builder.comment("");

        LAND_BASED_MOBS = builder
                .comment("List of mobs that should always be treated as land-based for teleportation (overrides default type check and water_based list).")
                .defineList("land_based_mobs", List.of(), obj -> obj instanceof String);
        builder.comment("");

        AMPHIBIAN_MOBS = builder
                .comment("List of amphibious mobs that prefer water when teleporting to a fully submerged controller")
                .defineList("amphibian_mobs", Arrays.asList("minecraft:turtle", "minecraft:frog", "minecraft:drowned"), obj -> obj instanceof String);
        SPEC = builder.build();
    }

    public static void register() {
        net.minecraftforge.fml.ModLoadingContext.get()
                .registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, SPEC, "mob_controller/mob_controller-teleport.toml");
    }
}