package net.xiaoyu.mob_controller.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import net.xiaoyu.mob_controller.network.NetWorkManager;
import net.xiaoyu.mob_controller.network.SyncLegionModePacket;
import net.xiaoyu.mob_controller.network.client.ClientPacketHandler;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 竞技之旗物品。
 * <p>用于管理军团模式：</p>
 * <ul>
 *   <li>左/右键切换队伍颜色（非潜行）</li>
 *   <li>潜行 + 左/右键批量启用/禁用军团模式</li>
 *   <li>右键点击受控生物可单独禁用其军团模式</li>
 *   <li>中键切换玩家自身军团模式</li>
 *   <li>工具提示实时显示当前队伍颜色</li>
 * </ul>
 */
public class LegionBannerItem extends Item {

    // 所有可用的队伍颜色（按循环顺序）
    private static final ChatFormatting[] COLORS = {
            ChatFormatting.BLACK, ChatFormatting.DARK_BLUE, ChatFormatting.DARK_GREEN,
            ChatFormatting.DARK_AQUA, ChatFormatting.DARK_RED, ChatFormatting.DARK_PURPLE,
            ChatFormatting.GOLD, ChatFormatting.GRAY, ChatFormatting.DARK_GRAY,
            ChatFormatting.BLUE, ChatFormatting.GREEN, ChatFormatting.AQUA,
            ChatFormatting.RED, ChatFormatting.LIGHT_PURPLE, ChatFormatting.YELLOW,
            ChatFormatting.WHITE
    };

    /** 颜色 -> RGB (ARGB, alpha = 0xFF) 映射表，供外部类访问 */
    public static final Map<ChatFormatting, Integer> COLOR_RGB = new HashMap<>();

    static {
        // 根据原版颜色定义 RGB 值（完全不透明，alpha = 255）
        COLOR_RGB.put(ChatFormatting.BLACK,        0xFF000000);
        COLOR_RGB.put(ChatFormatting.DARK_BLUE,    0xFF0000AA);
        COLOR_RGB.put(ChatFormatting.DARK_GREEN,   0xFF00AA00);
        COLOR_RGB.put(ChatFormatting.DARK_AQUA,    0xFF00AAAA);
        COLOR_RGB.put(ChatFormatting.DARK_RED,     0xFFAA0000);
        COLOR_RGB.put(ChatFormatting.DARK_PURPLE,  0xFFAA00AA);
        COLOR_RGB.put(ChatFormatting.GOLD,         0xFFFFAA00);
        COLOR_RGB.put(ChatFormatting.GRAY,         0xFFAAAAAA);
        COLOR_RGB.put(ChatFormatting.DARK_GRAY,    0xFF555555);
        COLOR_RGB.put(ChatFormatting.BLUE,         0xFF5555FF);
        COLOR_RGB.put(ChatFormatting.GREEN,        0xFF55FF55);
        COLOR_RGB.put(ChatFormatting.AQUA,         0xFF55FFFF);
        COLOR_RGB.put(ChatFormatting.RED,          0xFFFF5555);
        COLOR_RGB.put(ChatFormatting.LIGHT_PURPLE, 0xFFFF55FF);
        COLOR_RGB.put(ChatFormatting.YELLOW,       0xFFFFFF55);
        COLOR_RGB.put(ChatFormatting.WHITE,        0xFFF8F8FF);  // 略偏蓝的白，确保描边可见
    }

    public LegionBannerItem(Properties properties) {
        super(properties);
    }

    // ========== 队伍颜色存储与获取（基于玩家持久化 NBT） ==========

    /**
     * 获取玩家的队伍颜色。
     * @param player 目标玩家
     * @return 对应的 ChatFormatting 颜色，默认为红色
     */
    public static ChatFormatting getLegionColor(Player player) {
        CompoundTag data = player.getPersistentData();
        String colorName = data.getString("LegionColor");
        try {
            if (!colorName.isEmpty()) {
                return ChatFormatting.valueOf(colorName);
            }
        } catch (IllegalArgumentException ignored) {}
        return ChatFormatting.RED;
    }

    /**
     * 设置玩家的队伍颜色。
     * @param player 目标玩家
     * @param color  新的颜色
     */
    public static void setLegionColor(Player player, ChatFormatting color) {
        player.getPersistentData().putString("LegionColor", color.name());
    }

    /**
     * 循环切换玩家的队伍颜色。
     * @param player 目标玩家
     * @param delta  移动步数（正数向前，负数向后）
     */
    public static void cycleColor(Player player, int delta) {
        ChatFormatting current = getLegionColor(player);
        int index = -1;
        for (int i = 0; i < COLORS.length; i++) {
            if (COLORS[i] == current) {
                index = i;
                break;
            }
        }
        if (index == -1) index = 0;
        int next = (index + delta) % COLORS.length;
        if (next < 0) next += COLORS.length;
        setLegionColor(player, COLORS[next]);
    }

    /**
     * 获取队伍颜色的本地化显示名称，并应用对应的颜色样式。
     * @param color 队伍颜色
     * @return 带样式的文本组件
     */
    public static Component getTeamDisplayName(ChatFormatting color) {
        return switch (color) {
            case BLACK -> Component.translatable("mob_controller.team.black").withStyle(color);
            case DARK_BLUE -> Component.translatable("mob_controller.team.dark_blue").withStyle(color);
            case DARK_GREEN -> Component.translatable("mob_controller.team.dark_green").withStyle(color);
            case DARK_AQUA -> Component.translatable("mob_controller.team.dark_aqua").withStyle(color);
            case DARK_RED -> Component.translatable("mob_controller.team.dark_red").withStyle(color);
            case DARK_PURPLE -> Component.translatable("mob_controller.team.dark_purple").withStyle(color);
            case GOLD -> Component.translatable("mob_controller.team.gold").withStyle(color);
            case GRAY -> Component.translatable("mob_controller.team.gray").withStyle(color);
            case DARK_GRAY -> Component.translatable("mob_controller.team.dark_gray").withStyle(color);
            case BLUE -> Component.translatable("mob_controller.team.blue").withStyle(color);
            case GREEN -> Component.translatable("mob_controller.team.green").withStyle(color);
            case AQUA -> Component.translatable("mob_controller.team.aqua").withStyle(color);
            case RED -> Component.translatable("mob_controller.team.red").withStyle(color);
            case LIGHT_PURPLE -> Component.translatable("mob_controller.team.light_purple").withStyle(color);
            case YELLOW -> Component.translatable("mob_controller.team.yellow").withStyle(color);
            case WHITE -> Component.translatable("mob_controller.team.white").withStyle(color);
            default -> Component.literal(color.getName()).withStyle(color);
        };
    }

    /**
     * 将 ChatFormatting 颜色转换为整型 RGB（ARGB 格式，alpha = 255）。
     * @param color 队伍颜色
     * @return 对应的 ARGB 整数值
     */
    public static int getColorRGB(ChatFormatting color) {
        return COLOR_RGB.getOrDefault(color, 0xFFFF5555);
    }

    /**
     * 从 RGB 值反向获取 ChatFormatting（用于工具提示显示）。
     * @param rgb ARGB 颜色值
     * @return 对应的 ChatFormatting，若未找到则返回 null
     */
    private static ChatFormatting getChatFormattingFromRGB(int rgb) {
        for (Map.Entry<ChatFormatting, Integer> entry : COLOR_RGB.entrySet()) {
            if (entry.getValue().equals(rgb)) {
                return entry.getKey();
            }
        }
        return null;
    }

    // ========== 军团模式交互 ==========

    /**
     * 右键点击生物：关闭其军团模式（单体或批量）。
     * 潜行时批量关闭半径 32 格内所有受控生物的军团模式，否则仅关闭目标生物的军团模式。
     */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Mob mob)) return InteractionResult.PASS;
        if (player.level().isClientSide) {
            player.swing(hand);
            return InteractionResult.SUCCESS;
        }
        // 检查是否为当前玩家控制的生物
        if (MobControlledData.isControlledEntity(mob) &&
                player.getUUID().equals(MobControlledData.getControllerUUID(mob))) {
            if (player.isShiftKeyDown()) {
                // 批量禁用军团模式
                int count = MobControlledData.setLegionModeForAll(player, 32, false);
                player.displayClientMessage(Component.translatable("mob_controller.message.legion_disable_batch", count)
                        .withStyle(ChatFormatting.GOLD), true);
            } else {
                // 单体禁用
                MobControlledData.setLegionMode(mob, false);
                player.displayClientMessage(Component.translatable("mob_controller.message.legion_disable_single",
                        mob.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /**
     * 对空气右键：仅播放动画，具体逻辑由客户端鼠标事件包处理
     * （批量启用/禁用以及颜色切换由客户端监听鼠标事件发送数据包完成）
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.swing(hand);
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    // ========== 工具提示 ==========

    /**
     * 添加物品提示文本，实时显示当前队伍颜色（从客户端缓存获取，确保实时更新）。
     */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (level != null && level.isClientSide) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                // 从客户端缓存获取自己的颜色 RGB（确保与服务端和所有玩家同步）
                int rgb = ClientPacketHandler.getLegionColorRGB(player.getUUID());
                ChatFormatting color = getChatFormattingFromRGB(rgb);
                if (color == null) color = ChatFormatting.RED; // fallback

                Component teamName = getTeamDisplayName(color);
                tooltip.add(Component.translatable("mob_controller.tooltip.legion_banner", teamName));
                tooltip.add(Component.translatable("mob_controller.tooltip.legion_usage"));
                tooltip.add(Component.translatable("mob_controller.tooltip.legion_usage1"));
                tooltip.add(Component.translatable("mob_controller.tooltip.legion_usage2"));
                tooltip.add(Component.translatable("mob_controller.tooltip.legion_usage3"));
                tooltip.add(Component.translatable("mob_controller.tooltip.legion_usage4"));
                return;
            }
        }
        // 后备（例如在 JEI 中查看时无法获取玩家上下文）
        tooltip.add(Component.translatable("mob_controller.tooltip.legion_banner", "?"));
    }

    // ========== 玩家自身军团模式管理 ==========

    /**
     * 检查玩家是否处于军团模式。
     */
    public static boolean isPlayerInLegionMode(Player player) {
        return player.getPersistentData().getBoolean("mob_controller_player_legion_mode");
    }

    /**
     * 设置玩家的军团模式，并广播给所有客户端。
     * @param player 目标玩家（必须是 ServerPlayer）
     * @param enabled 是否启用
     */
    public static void setPlayerLegionMode(ServerPlayer player, boolean enabled) {
        boolean old = isPlayerInLegionMode(player);
        if (old == enabled) return;
        player.getPersistentData().putBoolean("mob_controller_player_legion_mode", enabled);
        // 同步给所有在线玩家
        NetWorkManager.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new SyncLegionModePacket(player.getUUID(), enabled));
        // 通知消息（使用 MutableComponent 以支持 withStyle）
        MutableComponent msg = enabled ?
                Component.translatable("mob_controller.message.legion_player_enable") :
                Component.translatable("mob_controller.message.legion_player_disable");
        player.displayClientMessage(msg.withStyle(ChatFormatting.GOLD), true);
        // 清理因切换而不再敌对的战斗目标（周围生物对玩家的仇恨清除）
        clearLegionTargetsForPlayer(player);
    }

    /**
     * 切换玩家的军团模式。
     */
    public static void togglePlayerLegionMode(ServerPlayer player) {
        setPlayerLegionMode(player, !isPlayerInLegionMode(player));
    }

    /**
     * 当玩家军团模式改变时，清除所有受控生物对该玩家的攻击目标。
     */
    private static void clearLegionTargetsForPlayer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        List<Mob> allControlled = level.getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(128),
                mob -> MobControlledData.isControlledEntity(mob));
        for (Mob mob : allControlled) {
            if (mob.getTarget() == player) {
                mob.setTarget(null);
                // 清理大脑记忆（猪灵、疣猪兽、僵尸疣猪兽）
                if (mob instanceof AbstractPiglin || mob instanceof Hoglin || mob instanceof Zoglin) {
                    Brain<?> brain = mob.getBrain();
                    brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
                    brain.eraseMemory(MemoryModuleType.ANGRY_AT);
                }
                // 监守者特殊处理
                else if (mob instanceof Warden warden) {
                    Brain<?> brain = warden.getBrain();
                    brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
                    brain.eraseMemory(MemoryModuleType.ROAR_TARGET);
                    // 清除愤怒系统中的目标
                    warden.clearAnger(player);
                }
            }
        }
    }
}