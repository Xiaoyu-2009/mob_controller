package net.xiaoyu.mob_controller;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.List;

/**
 * 模组通用配置类，持有所有通过 Forge Config 系统读写的配置项。
 *
 * <p>配置文件路径由 Forge 根据 {@link net.minecraftforge.fml.config.ModConfig.Type#COMMON} 类型
 * 自动在 {@code config/} 目录下生成，文件名格式为 {@code mob_controller-common.toml}。</p>
 *
 * <p>所有字段均为 {@code public static final}，可在任意线程安全地读取。</p>
 */
public class Config {
    /**
     * Forge 配置规格构建器，用于声明所有配置项。
     */
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static {
        BUILDER.push("Mob Controller Config");
    }

    /**
     * 自定义控制规则列表。
     * 格式：生物ID;物品ID;成功率(0~1);所需绝对血量(≤该值);是否消耗物品(true/false)
     * 示例：minecraft:zombie;minecraft:rotten_flesh;0.5;10;true
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CUSTOM_CONTROL_RULES = BUILDER
            .comment(
                    "!!! WARNING: EXPERIMENTAL TEST FEATURE - NOT GUARANTEED TO BE COMPATIBLE, MAY CAUSE GAME CRASHES OR DATA CORRUPTION !!!",
                    "IMPORTANT WARNING 1: If the specified item already has an interaction with the mob (e.g., wheat feeding cows, bones taming wolves),",
                    "this rule will PRIORITIZE the control logic over the original interaction. Right-clicking the mob with that item will",
                    "attempt to control it instead of feeding/breeding/taming. Choose your items carefully.",
                    "IMPORTANT WARNING 2: Items from this mod (namespace 'mob_controller', e.g., 'mob_controller:mob_controller') are FORBIDDEN.",
                    "If you use such an item, the rule will be silently ignored and will NOT work.",
                    "",
                    "Format: 'mob_id;item_id;chance;required_health;consume'",
                    "- mob_id: entity registry name, e.g., 'minecraft:cow'",
                    "- item_id: item registry name, e.g., 'minecraft:wheat' (cannot be 'mob_controller:xxx')",
                    "- chance: float 0.0-1.0, 1.0 = always succeed",
                    "- required_health: int, mob's current health must be <= this value to be controllable",
                    "- consume: true/false, whether to consume one item per attempt (regardless of success)",
                    "",
                    "Example: 'minecraft:cow;minecraft:wheat;1.0;10;true'")
            .defineList("custom_control_rules", List.of(), obj -> obj instanceof String);

    /**
     * 水平传送触发距离（单位：格）。
     * 当受控生物与控制者的水平距离超过此值时，强制传送回到控制者身边。
     * 默认值：14.0。
     */
    public static final ForgeConfigSpec.DoubleValue FOLLOW_TELEPORT_HORIZONTAL_DISTANCE = BUILDER
            .comment("Horizontal distance (blocks) that triggers teleportation when following. Default: 14.0")
            .defineInRange("follow_teleport_horizontal_distance", 14.0, 1.0, 256.0);

    /**
     * 垂直传送触发距离（单位：格）。
     * 当受控生物与控制者的垂直距离（绝对值）超过此值时，强制传送回到控制者身边。
     * 默认值：32.0。
     */
    public static final ForgeConfigSpec.DoubleValue FOLLOW_TELEPORT_VERTICAL_DISTANCE = BUILDER
            .comment("Vertical distance (blocks) that triggers teleportation when following. Default: 32.0")
            .defineInRange("follow_teleport_vertical_distance", 32.0, 1.0, 256.0);

    /**
     * 跟随移动触发距离（单位：格）。
     * 当受控生物与控制者的距离超过此值时，生物会主动向控制者移动（而非原地待命）。
     * 默认值：8.0。
     */
    public static final ForgeConfigSpec.DoubleValue FOLLOW_MOVE_TO_DISTANCE = BUILDER
            .comment("Distance (blocks) at which a controlled mob starts moving toward the controller when following. Default: 8.0")
            .defineInRange("follow_move_to_distance", 8.0, 1.0, 64.0);

    /**
     * 是否启用五谷杂粮的合成表。
     * true = 五谷杂粮可合成（生物控制器不可合成）；
     * false = 生物控制器可合成（五谷杂粮不可合成），默认为 false。
     */
    public static final ForgeConfigSpec.BooleanValue USE_GRAIN_RECIPE = BUILDER
            .comment("If true, the 'Grain' item will have a crafting recipe and the normal Inexhaustible Golden Stew will be uncraftable; if false, the opposite.")
            .define("use_grain_recipe", true);

    /**
     * 不可被控制的生物类型黑名单。
     *
     * <p>列表中的每一项均为实体注册名，格式为 {@code namespace:path}，
     * 例如 {@code "minecraft:wolf"}。默认值包含所有原版可驯服的生物，
     * 以避免与原版驯服机制冲突。</p>
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_MOBS = BUILDER
            .comment("List of mob that cannot be controlled")
            .defineList(
                    "blacklisted_mobs", Arrays.asList(
                            "minecraft:parrot",
                            "minecraft:wolf",
                            "minecraft:cat",
                            "minecraft:ocelot",
                            "minecraft:horse",
                            "minecraft:donkey",
                            "minecraft:mule",
                            "minecraft:llama",
                            "minecraft:trader_llama",
                            "minecraft:skeleton_horse",
                            "minecraft:zombie_horse",
                            "minecraft:camel",
                            "deep_aether:eots_segment",
                            "deep_aether:eots_controller",
                            "aether:sun_spirit",
                            "aether:slider",
                            "lost_aether_content:aerwhale_king"
                    ), obj -> obj instanceof String
            );

    /**
     * 在 STAY（停留）模式下需要进行坐标焊死（coordinate-weld）处理的特殊 AI 飞行生物列表。
     *
     * <p>这类生物在停留状态下会因为自身飞行 AI 持续漂移，
     * 因此需要每 tick 强制将其传送回停留位置以防止其移动。
     * 默认包含：恶魂、恼鬼、烈焰人、幻翼、蝙蝠。</p>
     *
     * @see net.xiaoyu.mob_controller.util.MobControlUtil#applyStayFlightCoordinateWeld(net.minecraft.world.entity.Mob)
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> STAY_WELDED_SPECIAL_AI_MOBS = BUILDER
            .comment("Special AI mobs that should be coordinate-welded in STAY mode")
            .defineList(
                    "stay_welded_special_ai_mobs", Arrays.asList(
                            "minecraft:ghast",
                            "minecraft:vex",
                            "minecraft:blaze",
                            "minecraft:phantom",
                            "minecraft:bat"
                    ), obj -> obj instanceof String
            );

    /**
     * 强制视为水生生物的列表（即使其原版类型不是 WATER）。
     * 列表中每一项为实体注册名，格式 "namespace:path"。
     * 当生物在此列表中时，传送落点一律选择水中。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WATER_BASED_MOBS = BUILDER
            .comment("List of mobs that should always be treated as water-based for teleportation (overrides default type check).")
            .defineList("water_based_mobs", List.of("alexsmobs:skelewag"), obj -> obj instanceof String);

    /**
     * 强制视为陆生生物的列表（即使其原版类型是 WATER 或在两栖列表中）。
     * 列表中每一项为实体注册名，格式 "namespace:path"。
     * 当生物在此列表中时，传送落点一律选择陆地（空气方块）。
     * 注意：此配置的优先级高于 water_based_mobs 和原版类型，但低于 amphibian_mobs 的特殊逻辑。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> LAND_BASED_MOBS = BUILDER
            .comment("List of mobs that should always be treated as land-based for teleportation (overrides default type check and water_based list).")
            .defineList("land_based_mobs", List.of(), obj -> obj instanceof String);

    /**
     * 两栖动物生物列表（用于跟随传送时的落点决策：在水中优先）。
     * 当控制者完全浸没在水中时，这些生物会被传送到水中安全位置。
     * 默认包含海龟（turtle）和青蛙（frog）。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> AMPHIBIAN_MOBS = BUILDER
            .comment("List of amphibious mobs that prefer water when teleporting to a fully submerged controller")
            .defineList(
                    "amphibian_mobs", Arrays.asList(
                            "minecraft:turtle",
                            "minecraft:frog",
                            "minecraft:drowned"
                    ), obj -> obj instanceof String
            );

    /**
     * 是否始终使控制尝试成功（即忽略成功率随机计算）。
     *
     * <p>若设为 {@code true}，则无论生物最大生命值多少，使用生物控制器物品时均可 100% 成功控制。
     * 建议调试时或服务器管理员测试时使用。默认值为 {@code false}。</p>
     */
    public static final ForgeConfigSpec.BooleanValue ALWAYS_SUCCESS = BUILDER
            .comment("Whether to always succeed in controlling mobs")
            .define("always_success", false);

    /**
     * 被控制生物是否会听从主人的指令去攻击其他玩家。
     *
     * <p>该配置仅影响“主人主动攻击玩家后，受控生物是否协同攻击”的行为，
     * 不影响主人/受控生物遭到其他玩家攻击时的防御反击逻辑。默认值为 {@code true}。</p>
     */
    public static final ForgeConfigSpec.BooleanValue CONTROLLED_MOBS_ATTACK_PLAYERS_ON_COMMAND = BUILDER
            .comment("Whether controlled mobs obey their owner's attack command against other players")
            .define("controlled_mobs_attack_players_on_command", true);

    /**
     * 受控生物在脱战后开始自动回血前需要等待的 tick 数。
     *
     * <p>默认值为 {@code 100}（5 秒）。设为 {@code 0} 表示一旦没有有效战斗目标就可立即开始回血。</p>
     */
    public static final ForgeConfigSpec.IntValue CONTROLLED_MOB_HEAL_OUT_OF_COMBAT_DELAY_TICKS = BUILDER
            .comment("Ticks a controlled mob must stay out of combat before auto-healing starts (100 ticks = 5 seconds)")
            .defineInRange("controlled_mob_heal_out_of_combat_delay_ticks", 100, 0, Integer.MAX_VALUE);

    /**
     * 判定为“高生命值生物”的生命值阈值。
     */
    public static final ForgeConfigSpec.IntValue HIGH_HEALTH_THRESHOLD = BUILDER
            .comment("The life value threshold for being classified as a 'high-life-value organism'")
            .defineInRange("high_health_threshold", 150, 1, Integer.MAX_VALUE);

    /**
     * 生物死亡后触发重生的延迟刻数（600 tick = 30 秒）。
     */
    public static final ForgeConfigSpec.IntValue RESPAWN_DELAY_TICKS = BUILDER
            .comment("The number of ticks that elapse before rebirth is triggered after the organism dies (600 ticks = 30 seconds)")
            .defineInRange("respawn_delay_ticks", 600, 100, Integer.MAX_VALUE);

    // 在 Config.java 中添加
    public static final ForgeConfigSpec.BooleanValue ENABLE_RESPAWN = BUILDER
            .comment("Enable or disable the respawn feature for controlled mobs. If false, controlled mobs will die normally without respawning.")
            .define("enable_respawn", true);

    /**
     * 生物的攻击力上限。当生物的基础攻击力（属性 attack_damage）达到或超过此值时，无法被控制。
     * 默认值 2147483647 表示实际上不限制（int 最大值）。
     */
    public static final ForgeConfigSpec.IntValue ATTACK_LIMIT = BUILDER
            .comment("Maximum attack damage (attribute attack_damage) allowed for a mob to be controllable. Mobs with attack damage >= this value cannot be controlled.")
            .defineInRange("attack_limit", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);

    /**
     * 生物的生命上限。当生物的最大生命值达到或超过此值时，无法被控制。
     * 默认值 2147483647 表示实际上不限制。
     */
    public static final ForgeConfigSpec.IntValue HEALTH_LIMIT = BUILDER
            .comment("Maximum health (max health) allowed for a mob to be controllable. Mobs with max health >= this value cannot be controlled.")
            .defineInRange("health_limit", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);

    /**
     * 满足驯服条件的生命百分比阈值（单位：百分比，支持小数，范围 0.0 ~ 100.0）。
     * <p>如果当前生命值低于最大生命值的这个百分比（例如 10.0 表示 10%，0.5 表示 0.5%），则可被控制。
     * 该条件与“驯服时需要怪物的血量”为二选一关系，满足任意一个即可开始控制尝试。</p>
     *
     */
    public static final ForgeConfigSpec.DoubleValue HEALTH_PERCENT_THRESHOLD = BUILDER
            .comment("Health percentage threshold (0.0 ~ 100.0). If (current health / max health) * 100 <= this value, the mob becomes eligible for control (alternative to required_health). Example: 10.0 = 10%, 0.5 = 0.5%")
            .defineInRange("health_percent_threshold", 0.01, 0.0, 100.0);

    /**
     * 驯服时需要怪物的固定血量阈值。如果当前生命值低于此值，则可被控制。
     * 与百分比条件为二选一关系。
     */
    public static final ForgeConfigSpec.IntValue REQUIRED_HEALTH = BUILDER
            .comment("Absolute health threshold. If current health <= this value, the mob becomes eligible for control (alternative to health_percent_threshold).")
            .defineInRange("required_health", 10, 1, Integer.MAX_VALUE);

    /**
     * 是否在直接右键切换控制模式（跟随/停留/游荡）时播放经验球拾取音效。
     * 仅控制者本人能听见。默认值为 true。
     */
    public static final ForgeConfigSpec.BooleanValue PLAY_SOUND_ON_MODE_SWITCH = BUILDER
            .comment("Whether to play the experience orb pickup sound when directly switching a controlled mob's mode (follow/stay/wander) via right-click. Only the controller hears it.")
            .define("play_sound_on_mode_switch", true);

    /**
     * 已构建完成的配置规格，在 {@link MobController} 构造器中通过
     * {@link net.minecraftforge.fml.ModLoadingContext#registerConfig} 注册。
     */
    public static final ForgeConfigSpec SPEC = BUILDER.build();
}