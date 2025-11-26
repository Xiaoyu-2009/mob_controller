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
                    "minecraft:parrot"
                ), obj -> obj instanceof String);
        
        ALWAYS_SUCCESS = BUILDER
                .comment("Whether control always succeeds")
                .define("always_success", false);
        
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}