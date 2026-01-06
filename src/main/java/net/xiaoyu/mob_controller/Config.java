package net.xiaoyu.mob_controller;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.*;

public class Config {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_MOBS;
    public static final ForgeConfigSpec.BooleanValue ALWAYS_SUCCESS;
    
    static {
        BUILDER.push("Mob Controller Config");
        
        BLACKLISTED_MOBS = BUILDER
                .comment("List of mob that cannot be controlled")
                .defineList("blacklisted_mobs", Arrays.asList(
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
                    "minecraft:zoglin"
                ), obj -> obj instanceof String);

        ALWAYS_SUCCESS = BUILDER
                .comment("Whether to always succeed in controlling mobs")
                .define("always_success", false);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}