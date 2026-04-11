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
                "minecraft:camel"
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
     * 是否始终使控制尝试成功（即忽略成功率随机计算）。
     *
     * <p>若设为 {@code true}，则无论生物最大生命值多少，使用生物控制器物品时均可 100% 成功控制。
     * 建议调试时或服务器管理员测试时使用。默认值为 {@code false}。</p>
     */
    public static final ForgeConfigSpec.BooleanValue ALWAYS_SUCCESS = BUILDER
        .comment("Whether to always succeed in controlling mobs")
        .define("always_success", false);

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
        .defineInRange("respawn_delay_ticks", 600, 1, Integer.MAX_VALUE);

    /**
     * 史莱姆/岩浆怪延迟重生体型策略。
     *
     * <p>设为 {@code true} 时，仅最小体型（size == 1）可进入延迟重生队列；
     * 设为 {@code false} 时，仅非最小体型（size > 1）可进入延迟重生队列。</p>
     */
    public static final ForgeConfigSpec.BooleanValue SLIME_RESPAWN_ONLY_MIN_SIZE = BUILDER
        .comment("If true, only smallest slimes/magma cubes (size == 1) can schedule respawn; if false, only non-smallest sizes (size > 1) can schedule respawn")
        .define("slime_respawn_only_min_size", true);

    /**
     * 已构建完成的配置规格，在 {@link MobController} 构造器中通过
     * {@link net.minecraftforge.fml.ModLoadingContext#registerConfig} 注册。
     */
    public static final ForgeConfigSpec SPEC = BUILDER.build();
}
