package net.xiaoyu.mob_controller;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * 模组客户端配置类，仅影响客户端表现（如音效、渲染等）。
 */
public class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue PLAY_SOUND_ON_MODE_SWITCH;

    static {
        ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
        BUILDER.push("Mob Controller Client Config");
        PLAY_SOUND_ON_MODE_SWITCH = BUILDER
                .comment("Whether to play the experience orb pickup sound when directly switching a controlled mob's mode (follow/stay/wander) via right-click. Only the controller hears it.")
                .define("play_sound_on_mode_switch", true);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC, "mob_controller/mob_controller-client.toml");
    }
}