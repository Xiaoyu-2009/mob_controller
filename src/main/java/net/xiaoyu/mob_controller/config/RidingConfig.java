package net.xiaoyu.mob_controller.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.List;

/**
 * 骑乘行为配置（可骑乘列表、速度、偏移等）。
 */
public class RidingConfig {
    public static final ForgeConfigSpec SPEC;

    /**
     * 可骑乘的陆地生物列表（原版骑乘控制），支持自定义跳跃高度。
     * 格式：'entity_id,jump_height'，例如 'minecraft:cow,1.2'。
     * 如果不写跳跃高度，则默认使用 0.42（玩家跳跃力度）。
     * 未在此列表中的实体默认不可直接骑乘。
     * 默认包含原硬编码的所有陆地生物，跳跃高度暂设为 0.42。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> LAND_RIDEABLE_MOBS;

    /**
     * 可骑乘的水生生物列表，采用守卫者/海豚的骑乘控制。
     * 格式：'entity_id,leave_water_dismount'，例如 'minecraft:dolphin,true'
     * - leave_water_dismount: 若为 true，当生物离开水时强制甩下骑手；false 则不会甩下。
     * 默认包含守卫者和海豚，离水均强制下马（true）。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> AQUATIC_RIDEABLE_MOBS;

    /**
     * 可骑乘的飞行生物列表，自定义飞行骑乘控制（任意地方可移动，空格上升）。
     * 格式：实体注册名，例如 "minecraft:phantom"
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FLYING_RIDEABLE_MOBS;

    /**
     * 可骑乘的两栖生物列表。
     * 在水中时采用水生控制（基于玩家俯仰角，空格上浮），在陆地上时采用陆地控制（可配置跳跃高度）。
     * 格式：'entity_id,jump_height'，例如 'minecraft:frog,0.8'。
     * 如果不写跳跃高度，则默认使用 0.42。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RIDE_AMPHIBIAN_MOBS;

    /**
     * 可骑乘的“飞行陆地”生物列表。
     * 这类生物默认在地面行走（移动控制同陆地），按住空格键可上升（类似自由飞行）。
     * 格式：仅实体注册名，例如 'minecraft:phantom'。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RIDE_FLYING_LAND_MOBS;

    /**
     * 自定义骑乘速度配置（仅用于飞行、水生、两栖及飞行陆地生物的非陆地状态）。
     * 格式：'entity_id,speed'，speed 为每 tick 移动的距离（双精度浮点数）。
     * 示例：'minecraft:phantom,0.5' 表示幻翼每 tick 移动 0.5 格。
     * 未在此列表中的实体使用默认速度 0.35。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RIDE_CUSTOM_SPEED;

    /**
     * 骑乘偏移配置（支持 X、Y、Z 三个方向的偏移，偏移值相对于坐骑的本地坐标系）。
     * 格式：实体注册名,偏移X,偏移Y,偏移Z（均为浮点数，单位：格）
     * - 正值 X：向右偏移（坐骑右侧）
     * - 负值 X：向左偏移（坐骑左侧）
     * - 正值 Y：向上抬升
     * - 负值 Y：向下降低
     * - 正值 Z：向前偏移（坐骑前方）
     * - 负值 Z：向后偏移（坐骑后方）
     * 向后兼容：若只提供一个数值（如 minecraft:horse,0.2）则自动视为 Y 偏移，X 和 Z 保持为 0。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RIDE_POSITION_Y_OFFSET;

    /**
     * 是否禁止玩家攻击自己当前骑乘的坐骑（无论该坐骑是否受控）。
     * true = 玩家无法伤害自己正骑着的实体；false = 可以正常攻击。
     */
    public static final ForgeConfigSpec.BooleanValue PREVENT_PLAYER_ATTACK_OWN_MOUNT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("===================================================");
        builder.comment("  Riding Configuration");
        builder.comment("===================================================");
        builder.comment("");

        LAND_RIDEABLE_MOBS = builder
                .comment("List of land mobs that can be directly ridden (standard vanilla riding control). Supports custom jump height.",
                        "Format: 'entity_id,jump_height' (jump_height is double, optional). Example: 'minecraft:cow,1.2'.")
                .defineList("land_rideable_mobs", List.of(
                        "minecraft:cow,0.42", "minecraft:sheep,0.42", "minecraft:panda,0.42",
                        "minecraft:polar_bear,0.42", "minecraft:goat,0.42", "minecraft:hoglin,0.42",
                        "minecraft:zoglin,0.42", "minecraft:ravager,0.42", "minecraft:sniffer,0.42"
                ), obj -> obj instanceof String);
        builder.comment("");

        AQUATIC_RIDEABLE_MOBS = builder
                .comment("List of aquatic mobs that use guardian/dolphin riding control.",
                        "Format: 'entity_id,leave_water_dismount' (boolean), e.g., 'minecraft:dolphin,true'")
                .defineList("aquatic_rideable_mobs", List.of(
                        "minecraft:guardian,true", "minecraft:elder_guardian,true", "minecraft:dolphin,false"
                ), obj -> obj instanceof String);
        builder.comment("");

        FLYING_RIDEABLE_MOBS = builder
                .comment("List of flying mobs that can be ridden with custom flying control (move in look direction, space to ascend).",
                        "Format: 'entity_id', e.g., 'minecraft:phantom'")
                .defineList("flying_rideable_mobs", List.of(), obj -> obj instanceof String);
        builder.comment("");

        RIDE_AMPHIBIAN_MOBS = builder
                .comment("Amphibious rideable mobs. In water: aquatic control (pitch-based, space to ascend). On land: land control with configurable jump height.",
                        "Format: 'entity_id,jump_height' (optional). Example: 'minecraft:frog,0.8'.")
                .defineList("ride_amphibian_mobs", List.of(), obj -> obj instanceof String);
        builder.comment("");

        RIDE_FLYING_LAND_MOBS = builder
                .comment("Flying-land rideable mobs. Walk on ground like land mobs, press space to ascend freely.",
                        "Format: 'entity_id', e.g., 'minecraft:phantom'.")
                .defineList("ride_flying_land_mobs", List.of(), obj -> obj instanceof String);
        builder.comment("");

        RIDE_CUSTOM_SPEED = builder
                .comment("Custom riding speed for flying, aquatic, amphibian (in water) and flying-land (in air) mobs.",
                        "Format: 'entity_id,speed' where speed is double (blocks per tick).",
                        "Example: 'minecraft:phantom,0.5'",
                        "If not listed, default speed is 0.35.")
                .defineList("ride_custom_speed", List.of(), obj -> obj instanceof String);
        builder.comment("");

        RIDE_POSITION_Y_OFFSET = builder
                .comment("Custom rider offset (X,Y,Z) for specific mounts, relative to mount's local axes. Format: 'entity_id,offsetX,offsetY,offsetZ' (all doubles).",
                        "Example: 'minecraft:horse,0.2,0.1,-0.1' (right +0.2, up +0.1, back -0.1).",
                        "Backward compatible: 'minecraft:horse,0.2' means only Y offset 0.2, X=Z=0.")
                .defineList("ride_position_y_offset", List.of(
                        "minecraft:ghast,0,1.0,2",
                        "alexsmobs:bison,0,0.6,0",
                        "alexsmobs:cachalot_whale,0,3,0",
                        "alexsmobs:orca,0,-0.2,1"
                ), obj -> obj instanceof String);
        builder.comment("");

        PREVENT_PLAYER_ATTACK_OWN_MOUNT = builder
                .comment("Prevent players from damaging the entity they are currently riding.", "Default: true")
                .define("prevent_player_attack_own_mount", true);
        SPEC = builder.build();
    }

    public static void register() {
        net.minecraftforge.fml.ModLoadingContext.get()
                .registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, SPEC, "mob_controller/mob_controller-riding.toml");
    }
}