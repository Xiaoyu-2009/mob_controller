package net.xiaoyu.mob_controller.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.Arrays;
import java.util.List;

/**
 * 功能开关、限制与数值调整配置。
 * 本配置文件位于：mob_controller/mob_controller-features.toml
 */
public class FeatureConfig {
    public static final ForgeConfigSpec SPEC;

    /**
     * 不可被控制的生物类型黑名单。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_MOBS;

    /**
     * 列表中的生物即使拥有主人或驯服标记，也会被 hasOwnerOrTameTag 判定为 false。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> IGNORE_OWNER_TAG_MOBS;

    /**
     * 在 STAY（停留）模式下需要进行坐标焊死处理的特殊 AI 飞行生物列表。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> STAY_WELDED_SPECIAL_AI_MOBS;

    /**
     * 是否始终使控制尝试成功。
     */
    public static final ForgeConfigSpec.BooleanValue ALWAYS_SUCCESS;

    /**
     * 被控制生物是否会听从主人的指令去攻击其他玩家。
     */
    public static final ForgeConfigSpec.BooleanValue CONTROLLED_MOBS_ATTACK_PLAYERS_ON_COMMAND;

    /**
     * 受控生物在脱战后开始自动回血前需要等待的 tick 数。
     */
    public static final ForgeConfigSpec.IntValue CONTROLLED_MOB_HEAL_OUT_OF_COMBAT_DELAY_TICKS;

    /**
     * 判定为“高生命值生物”的生命值阈值。
     */
    public static final ForgeConfigSpec.IntValue HIGH_HEALTH_THRESHOLD;

    /**
     * 生物死亡后触发重生的延迟刻数。
     */
    public static final ForgeConfigSpec.IntValue RESPAWN_DELAY_TICKS;

    /**
     * 是否启用重生功能。
     */
    public static final ForgeConfigSpec.BooleanValue ENABLE_RESPAWN;

    /**
     * 生物的攻击力上限。
     */
    public static final ForgeConfigSpec.IntValue ATTACK_LIMIT;

    /**
     * 生物的生命上限。
     */
    public static final ForgeConfigSpec.IntValue HEALTH_LIMIT;

    /**
     * 满足驯服条件的生命百分比阈值。
     */
    public static final ForgeConfigSpec.DoubleValue HEALTH_PERCENT_THRESHOLD;

    /**
     * 驯服时需要怪物的固定血量阈值。
     */
    public static final ForgeConfigSpec.IntValue REQUIRED_HEALTH;

    /**
     * 是否允许受控流浪商人拥有无限交易。
     */
    public static final ForgeConfigSpec.BooleanValue INFINITE_TRADES_FOR_CONTROLLED_WANDERING_TRADER;

    /**
     * 雪球击退效果模式（极小伤害+即时回血）
     */
    public static final ForgeConfigSpec.EnumValue<SnowballKnockbackOption> SNOWBALL_KNOCKBACK_MODE;

    /**
     * 是否允许着火雪球使目标着火。
     */
    public static final ForgeConfigSpec.BooleanValue SNOWBALL_FIRE_HIT;

    /**
     * 是否在重生开启时阻止受控生物掉落任何物品（包括装备、战利品表物品、经验值）。
     */
    public static final ForgeConfigSpec.BooleanValue PREVENT_DROPS_ON_RESPAWN;

    /**
     * 自定义生物最大数量限制（覆盖高生命值限制）
     * 格式：'生物注册名,最大数量'，例如 'minecraft:iron_golem,3'
     * 若生物在此列表中，则不受高生命值阈值判断，且每个玩家最多控制指定数量。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CUSTOM_MAX_COUNTS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("===================================================");
        builder.comment("  Features & Limitations");
        builder.comment("===================================================");
        builder.comment("");

        BLACKLISTED_MOBS = builder
                .comment("List of mob that cannot be controlled.",
                        "The default mobs in the blacklist (except vanilla ones) are added due to serious code conflicts and incompatibility. Please remove them with caution.")
                .defineList("blacklisted_mobs", Arrays.asList(
                        "minecraft:ender_dragon",
                        "deep_aether:eots_segment",
                        "deep_aether:eots_controller",
                        "aether:sun_spirit",
                        "aether:slider",
                        "lost_aether_content:aerwhale_king",
                        "alexsmobs:centipede_body",
                        "alexsmobs:centipede_tail",
                        "alexsmobs:void_worm",
                        "alexsmobs:void_worm_part",
                        "twilightforest:hydra"
                ), obj -> obj instanceof String);
        builder.comment("");

        IGNORE_OWNER_TAG_MOBS = builder
                .comment("Mobs in this list will be considered as having no owner/tame tag even if they normally do.",
                        "Format: entity registry name, e.g., 'minecraft:wolf'.",
                        "This overrides the default owner/tame detection.")
                .defineList("ignore_owner_tag_mobs", Arrays.asList(
                        "alexscaves:grottoceratops",
                        "alexscaves:relicheirus",
                        "alexscaves:luxtructosaurus",
                        "alexscaves:atlatitan",
                        "alexsmobs:orca",
                        "bosses_of_mass_destruction:lich",
                        "bosses_of_mass_destruction:void_blossom",
                        "bosses_of_mass_destruction:obsidilith",
                        "bosses_of_mass_destruction:gauntlet"
                ), obj -> obj instanceof String);
        builder.comment("");

        STAY_WELDED_SPECIAL_AI_MOBS = builder
                .comment("Special AI mobs that should be coordinate-welded in STAY mode")
                .defineList("stay_welded_special_ai_mobs", Arrays.asList(
                        "minecraft:ghast", "minecraft:vex", "minecraft:blaze", "minecraft:phantom", "minecraft:bat"
                ), obj -> obj instanceof String);
        builder.comment("");

        ALWAYS_SUCCESS = builder
                .comment("Whether to always succeed in controlling mobs")
                .define("always_success", false);
        builder.comment("");

        CONTROLLED_MOBS_ATTACK_PLAYERS_ON_COMMAND = builder
                .comment("Whether controlled mobs obey their owner's attack command against other players")
                .define("controlled_mobs_attack_players_on_command", true);
        builder.comment("");

        CONTROLLED_MOB_HEAL_OUT_OF_COMBAT_DELAY_TICKS = builder
                .comment("Ticks a controlled mob must stay out of combat before auto-healing starts (100 ticks = 5 seconds)")
                .defineInRange("controlled_mob_heal_out_of_combat_delay_ticks", 100, 0, Integer.MAX_VALUE);
        builder.comment("");

        HIGH_HEALTH_THRESHOLD = builder
                .comment("The life value threshold for being classified as a 'high-life-value organism'")
                .defineInRange("high_health_threshold", 150, 1, Integer.MAX_VALUE);
        builder.comment("");

        RESPAWN_DELAY_TICKS = builder
                .comment("The number of ticks that elapse before rebirth is triggered after the organism dies (600 ticks = 30 seconds)")
                .defineInRange("respawn_delay_ticks", 600, 100, Integer.MAX_VALUE);
        builder.comment("");

        ENABLE_RESPAWN = builder
                .comment("Enable or disable the respawn feature for controlled mobs. If false, controlled mobs will die normally without respawning.")
                .define("enable_respawn", true);
        builder.comment("");

        ATTACK_LIMIT = builder
                .comment("Maximum attack damage (attribute attack_damage) allowed for a mob to be controllable. Mobs with attack damage >= this value cannot be controlled.")
                .defineInRange("attack_limit", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
        builder.comment("");

        HEALTH_LIMIT = builder
                .comment("Maximum health (max health) allowed for a mob to be controllable. Mobs with max health >= this value cannot be controlled.")
                .defineInRange("health_limit", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
        builder.comment("");

        HEALTH_PERCENT_THRESHOLD = builder
                .comment("Health percentage threshold (0.0 ~ 100.0). If (current health / max health) * 100 <= this value, the mob becomes eligible for control (alternative to required_health). Example: 10.0 = 10%, 0.5 = 0.5%")
                .defineInRange("health_percent_threshold", 0.01, 0.0, 100.0);
        builder.comment("");

        REQUIRED_HEALTH = builder
                .comment("Absolute health threshold. If current health <= this value, the mob becomes eligible for control (alternative to health_percent_threshold).")
                .defineInRange("required_health", 10, 1, Integer.MAX_VALUE);
        builder.comment("");

        INFINITE_TRADES_FOR_CONTROLLED_WANDERING_TRADER = builder
                .comment("Enable infinite trades for controlled wandering traders (reset uses to 0 after each trade).")
                .define("infinite_trades_for_controlled_wandering_trader", true);
        builder.comment("");

        // 雪球击退效果配置（极小伤害+即时回血）
        SNOWBALL_KNOCKBACK_MODE = builder
                .comment("Control snowball knockback effect (0.000001 damage + instant heal).",
                        "Options:",
                        "  DISABLED                     - No effect, vanilla behavior.",
                        "  CONTROLLED_SNOW_GOLEM_ONLY   - Only snowballs thrown by controlled snow golems deal the tiny damage + heal (default).",
                        "  SNOW_GOLEM_ONLY              - All snow golems (controlled or not) throw snowballs with the effect.",
                        "  ALL_SNOWBALLS                - Every snowball (from any source) has the effect.")
                .defineEnum("snowball_knockback_mode", SnowballKnockbackOption.CONTROLLED_SNOW_GOLEM_ONLY);
        builder.comment("");

        // 着火雪球使目标着火开关
        SNOWBALL_FIRE_HIT = builder
                .comment("If true, a snowball that is on fire will set the hit entity on fire for 5 seconds.",
                        "Default: true")
                .define("snowball_fire_hit", true);
        builder.comment("");

        PREVENT_DROPS_ON_RESPAWN = builder
                .comment("If true, controlled mobs will drop no loot, equipment, or experience orbs when they die while respawn is enabled (enable_respawn must be true).",
                        "This prevents item duplication issues when respawning.",
                        "Default: true")
                .define("prevent_drops_on_respawn", true);
        builder.comment("");

        CUSTOM_MAX_COUNTS = builder
                .comment("Custom max counts for specific mobs (overrides high health limit).",
                        "Format: 'entity_id,max_count' (e.g., 'minecraft:iron_golem,3').",
                        "If a mob is listed here, it is not considered 'high health' and can be controlled up to max_count per player.",
                        "Set max_count = 0 to completely disallow controlling this mob.")
                .defineList("custom_max_counts", List.of(), obj -> obj instanceof String);

        SPEC = builder.build();
    }

    /**
     * 注册本配置文件到 Forge 配置系统。
     */
    public static void register() {
        ModLoadingContext.get()
                .registerConfig(ModConfig.Type.COMMON, SPEC, "mob_controller/mob_controller-features.toml");
    }

    /**
     * 雪球击退效果的可选模式枚举。
     */
    public enum SnowballKnockbackOption {
        DISABLED,
        CONTROLLED_SNOW_GOLEM_ONLY,
        SNOW_GOLEM_ONLY,
        ALL_SNOWBALLS
    }
}