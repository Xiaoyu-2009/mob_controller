package net.xiaoyu.mob_controller.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.List;

/**
 * 物品相关配置（最大乘客数、配方开关）。
 */
public class ItemConfig {
    public static final ForgeConfigSpec SPEC;

    /**
     * 为特定实体类型单独配置最大乘客数。
     * 格式：'entity_id;max_count'，例如 'minecraft:camel;2'。
     * 未在此列表中的实体默认最大乘客数为 1。
     *
     * ⚠️ NOTE: 此配置仅对“骑乘之笛”（Ride Command item）有效，
     * 不影响其他骑乘方式（如空手右键直接骑乘、原版马鞍等）。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MAX_RIDERS_PER_MOUNT;

    /**
     * 是否启用五谷杂粮的合成表。
     * true = 五谷杂粮可合成（生物控制器不可合成）；
     * false = 生物控制器可合成（五谷杂粮不可合成），默认为 false。
     */
    public static final ForgeConfigSpec.BooleanValue USE_GRAIN_RECIPE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("===================================================");
        builder.comment("  Items & Recipes");
        builder.comment("===================================================");
        builder.comment("");

        MAX_RIDERS_PER_MOUNT = builder
                .comment(
                        "Define max number of riders that can mount a specific entity type. Format: 'entity_id;max_count'.",
                        "Example: 'minecraft:camel;2' (camel supports 2 riders).",
                        "If an entity is not listed, it defaults to 1 rider.",
                        "",
                        "NOTE: This configuration only affects the 'Ride Command' item.",
                        "It does NOT apply to vanilla riding (e.g., using saddle on horse) or direct right-click riding.",
                        ""
                )
                .defineList("max_riders_per_mount", List.of("minecraft:camel;2"), obj -> obj instanceof String);
        builder.comment("");

        USE_GRAIN_RECIPE = builder
                .comment("If true, the 'Grain' item will have a crafting recipe and the normal Inexhaustible Golden Stew will be uncraftable; if false, the opposite.")
                .define("use_grain_recipe", true);
        SPEC = builder.build();
    }

    public static void register() {
        net.minecraftforge.fml.ModLoadingContext.get()
                .registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, SPEC, "mob_controller/mob_controller-items.toml");
    }
}