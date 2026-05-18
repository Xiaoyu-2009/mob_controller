package net.xiaoyu.mob_controller.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 骑乘令物品。
 *
 * <p>使用方式：</p>
 * <ul>
 *   <li>左键点击一个受控生物或驯服宠物 → 将其选为“骑手”（物品会记录该生物的UUID）。</li>
 *   <li>再次左键点击另一个可作为坐骑的生物 → 让之前选中的骑手骑上该坐骑。</li>
 *   <li>对空气右键 → 清除当前选中的骑手。</li>
 *   <li>对已骑乘的受控生物右键 → 使其从坐骑上下来。</li>
 * </ul>
 *
 * <p>坐骑的乘客数量受到配置文件限制（max_riders_per_mount），未配置的实体默认可承载 1 个乘客，例如骆驼默认配置为 2。</p>
 */
public class RideCommandItem extends Item {

    private static final String TAG_RIDER_UUID = "RideCommandRiderUUID";
    private static final String TAG_RIDER_NAME  = "RideCommandRiderName";   // 存储骑手显示名称

    // ========== 乘客数量配置缓存 ==========
    private static final Map<String, Integer> MAX_RIDERS_CACHE = new ConcurrentHashMap<>();
    private static boolean maxRidersCacheInitialized = false;

    public RideCommandItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level worldIn, BlockPos pos, Player player) {
        return !player.isCreative();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (hasRider(stack)) {
                clearRider(stack);
                MobControlUtil.showMessageToPlayer(player, Component.empty(), "mob_controller.message.ride_reset", new Object[]{}, ChatFormatting.YELLOW);
            } else {
                player.displayClientMessage(Component.translatable("mob_controller.message.ride_no_selection").withStyle(ChatFormatting.RED), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        Level level = player.level();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        // 右键实体：让目标从坐骑上下来（仅当目标受控且控制者为玩家）
        if (target instanceof Mob mob && MobControlledData.isControlledEntity(mob)) {
            UUID controllerUUID = MobControlledData.getControllerUUID(mob);
            if (controllerUUID != null && controllerUUID.equals(player.getUUID())) {
                if (mob.isPassenger()) {
                    mob.stopRiding();
                    MobControlUtil.showMessageToPlayer(player, Component.empty(), "mob_controller.message.ride_dismounted", new Object[]{mob.getDisplayName()}, ChatFormatting.GREEN);
                } else {
                    MobControlUtil.showMessageToPlayer(player, Component.empty(), "mob_controller.message.ride_not_riding", new Object[]{mob.getDisplayName()}, ChatFormatting.YELLOW);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("mob_controller.tooltip.ride_command").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("mob_controller.tooltip.ride_command.desc1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("mob_controller.tooltip.ride_command.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("mob_controller.tooltip.ride_command.desc3").withStyle(ChatFormatting.GRAY));
        if (hasRider(stack)) {
            String riderName = getRiderName(stack);  // 获取存储的生物名称
            tooltip.add(Component.translatable("mob_controller.tooltip.ride_command.selected", riderName).withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("mob_controller.tooltip.ride_command.selected_invalid").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasRider(stack);
    }

    // ========== 骑手存储逻辑 ==========

    private static boolean hasRider(ItemStack stack) {
        return stack.getOrCreateTag().contains(TAG_RIDER_UUID);
    }

    @Nullable
    private static UUID getRiderUUID(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_RIDER_UUID)) {
            return tag.getUUID(TAG_RIDER_UUID);
        }
        return null;
    }

    /** 获取骑手显示名称（优先使用存储的文本） */
    private static String getRiderName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_RIDER_NAME)) {
            return tag.getString(TAG_RIDER_NAME);
        }
        return Component.translatable("mob_controller.tooltip.ride_command.unknown").getString();
    }

    /** 设置骑手UUID及显示名称 */
    private static void setRiderData(ItemStack stack, UUID uuid, String name) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(TAG_RIDER_UUID, uuid);
        tag.putString(TAG_RIDER_NAME, name);
    }

    private static void clearRider(ItemStack stack) {
        stack.getOrCreateTag().remove(TAG_RIDER_UUID);
        stack.getOrCreateTag().remove(TAG_RIDER_NAME);
    }

    // ========== 骑手有效性判断 ==========

    private static boolean isRiderValid(Player player, Mob mob) {
        // 玩家控制的生物
        if (MobControlledData.isControlledEntity(mob)) {
            UUID controller = MobControlledData.getControllerUUID(mob);
            return controller != null && controller.equals(player.getUUID());
        }
        // 主人相同的驯服宠物
        if (mob instanceof TamableAnimal tamable) {
            if (tamable.isTame()) {
                UUID ownerUUID = tamable.getOwnerUUID();
                return ownerUUID != null && ownerUUID.equals(player.getUUID());
            }
        }
        return false;
    }

    private static boolean isValidMount(Mob mount, Player player) {
        // 马类：通过NBT中的Tame标签判断（马、驴、骡、骷髅马、僵尸马）
        CompoundTag nbt = mount.saveWithoutId(new CompoundTag());
        if (nbt.contains("Tame")) {
            return true;
        }
        // 非马类：受控生物或驯服宠物
        if (MobControlledData.isControlledEntity(mount)) {
            UUID controller = MobControlledData.getControllerUUID(mount);
            return controller != null && controller.equals(player.getUUID());
        }
        if (mount instanceof TamableAnimal tamable) {
            return tamable.isTame() && tamable.getOwnerUUID() != null && tamable.getOwnerUUID().equals(player.getUUID());
        }
        return false;
    }

    // ========== 左键选择/骑乘逻辑（由事件调用） ==========

    /**
     * 处理骑乘令的左键点击逻辑（由 MobControllerEvent 调用）。
     *
     * @param player 操作玩家
     * @param target 被左键的实体
     * @param stack  玩家主手的骑乘令物品栈
     * @return true 表示已处理该事件，应取消原版攻击伤害
     */
    public static boolean handleLeftClick(ServerPlayer player, Entity target, ItemStack stack) {
        Level level = player.level();
        if (!(target instanceof Mob mob)) {
            MobControlUtil.showMessageToPlayer(player, Component.empty(), "mob_controller.message.ride_not_mob", new Object[]{}, ChatFormatting.RED);
            return true;
        }

        UUID riderUUID = getRiderUUID(stack);
        if (riderUUID != null) {
            // 已有选中的骑手 -> 尝试骑乘
            if (!(level instanceof ServerLevel serverLevel)) {
                return true;
            }
            Entity riderEntity = serverLevel.getEntity(riderUUID);
            if (!(riderEntity instanceof Mob riderMob) || !riderMob.isAlive() || !isRiderValid(player, riderMob)) {
                clearRider(stack);
                MobControlUtil.showMessageToPlayer(player, Component.empty(), "mob_controller.message.ride_rider_lost", new Object[]{}, ChatFormatting.RED);
                return true;
            }

            if (!isValidMount(mob, player)) {
                MobControlUtil.showMessageToPlayer(player, Component.empty(), "mob_controller.message.ride_invalid_mount", new Object[]{}, ChatFormatting.RED);
                return true;
            }

            if (riderMob == mob) {
                MobControlUtil.showMessageToPlayer(player, Component.empty(), "mob_controller.message.ride_self", new Object[]{}, ChatFormatting.RED);
                clearRider(stack);
                return true;
            }

            // ========== 新增：检查坐骑的最大乘客数量限制 ==========
            int maxRiders = getMaxRidersForMount(mob);
            if (mob.getPassengers().size() >= maxRiders) {
                MobControlUtil.showMessageToPlayer(player, Component.empty(),
                        "mob_controller.message.ride_mount_full",
                        new Object[]{maxRiders, mob.getDisplayName()},  // 注意顺序：数量在前，坐骑名称在后
                        ChatFormatting.RED);
                clearRider(stack);
                return true;
            }

            if (riderMob.isPassenger()) {
                riderMob.stopRiding();
            }
            riderMob.startRiding(mob, true);
            MobControlUtil.showMessageToPlayer(player, Component.empty(), "mob_controller.message.ride_performed", new Object[]{riderMob.getDisplayName(), mob.getDisplayName()}, ChatFormatting.GREEN);
            clearRider(stack);
            return true;
        } else {
            // 未有选中的骑手 -> 检查当前生物是否可作为骑手
            if (!isRiderValid(player, mob)) {
                MobControlUtil.showMessageToPlayer(player, Component.empty(), "mob_controller.message.ride_selector_not_controlled", new Object[]{}, ChatFormatting.RED);
                return true;
            }
            // 存储骑手UUID和显示名称（用于工具提示）
            String displayName = mob.getDisplayName().getString();
            setRiderData(stack, mob.getUUID(), displayName);
            MobControlUtil.showMessageToPlayer(player, Component.empty(), "mob_controller.message.ride_selected", new Object[]{mob.getDisplayName()}, ChatFormatting.GOLD);
            return true;
        }
    }

    // ========== 新增：乘客数量限制辅助方法 ==========

    /**
     * 从配置文件加载每个实体类型的最大乘客数（格式："entity_id;max_count"）。
     * 未在配置中列出的实体默认最大乘客数为 1。
     */
    private static void ensureMaxRidersCache() {
        if (maxRidersCacheInitialized) return;
        synchronized (RideCommandItem.class) {
            if (maxRidersCacheInitialized) return;
            MAX_RIDERS_CACHE.clear();
            for (String entry : Config.MAX_RIDERS_PER_MOUNT.get()) {
                String[] parts = entry.split(";");
                if (parts.length == 2) {
                    try {
                        MAX_RIDERS_CACHE.put(parts[0], Integer.parseInt(parts[1]));
                    } catch (NumberFormatException ignored) {
                        // 忽略格式错误的条目
                    }
                }
            }
            maxRidersCacheInitialized = true;
        }
    }

    /**
     * 获取某个实体作为坐骑时允许的最大乘客数（未配置则默认为 1）。
     */
    private static int getMaxRidersForMount(Entity mount) {
        ensureMaxRidersCache();
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(mount.getType());
        if (key != null) {
            return MAX_RIDERS_CACHE.getOrDefault(key.toString(), 1);
        }
        return 1;
    }

    /**
     * 用于配置重载时重置乘客数量缓存（可选）。
     */
    public static void resetMaxRidersCache() {
        maxRidersCacheInitialized = false;
        MAX_RIDERS_CACHE.clear();
    }
}