package net.xiaoyu.mob_controller.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.List;

/**
 * 通用控制规则配置（实验性功能）。
 */
public class ControlConfig {
    public static final ForgeConfigSpec SPEC;
    /**
     * 自定义控制规则列表。
     * 格式：生物ID;物品ID;成功率(0~1);所需绝对血量(≤该值);是否消耗物品(true/false)
     * 示例：minecraft:zombie;minecraft:rotten_flesh;0.5;10;true
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CUSTOM_CONTROL_RULES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("===================================================");
        builder.comment("  Custom Control Rules (EXPERIMENTAL)");
        builder.comment("===================================================");
        builder.comment("");

        CUSTOM_CONTROL_RULES = builder
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
                        "Example: 'minecraft:cow;minecraft:wheat;1.0;10;true'"
                )
                .defineList("custom_control_rules", List.of(), obj -> obj instanceof String);
        SPEC = builder.build();
    }

    public static void register() {
        net.minecraftforge.fml.ModLoadingContext.get()
                .registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, SPEC, "mob_controller/mob_controller-control.toml");
    }
}