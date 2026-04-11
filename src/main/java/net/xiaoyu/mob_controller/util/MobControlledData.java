package net.xiaoyu.mob_controller.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.util.LazyOptional;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.capability.MobControlCapability;
import net.xiaoyu.mob_controller.capability.MobControlCapabilityProvider;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 维护“被控制生物”的运行时数据与全局辅助逻辑。
 *
 * <p>该类负责：</p>
 * <ul>
 *   <li>记录玩家已控制的高生命值生物类型，限制同类重复控制；</li>
 *   <li>读写生物控制状态（控制者、模式、系统攻击标记）；</li>
 *   <li>安排并处理生物死亡后的延迟重生。</li>
 * </ul>
 */
public class MobControlledData {
    /**
     * 玩家 -> 已控制的高生命值生物类型计数。
     */
    private static final Map<UUID, Map<EntityType<?>, Integer>> PLAYER_CONTROLLED_HIGH_HEALTH_MOBS = new ConcurrentHashMap<>();
    /**
     * 待执行的延迟重生任务。键为死亡生物 UUID。
     */
    private static final Map<UUID, PendingRespawnData> PENDING_RESPAWNS = new ConcurrentHashMap<>();
    private static final String PENDING_RESPAWN_TAG = "pending_respawns";
    private static final String PENDING_RESPAWN_DATA_DIR = "mob_controller";
    private static final String PENDING_RESPAWN_FILE = "pending_respawns.dat";
    @Nullable
    private static Path loadedPendingRespawnFile;

    /**
     * 将生物加入控制状态，并初始化为“跟随”模式。
     *
     * @param controllerUUID 控制者玩家 UUID
     * @param mob            目标生物
     */
    public static void addControlledMob(UUID controllerUUID, Mob mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        capability.ifPresent(cap -> {
            cap.setControllerUUID(controllerUUID);
            cap.setControlMode(ControlMode.FOLLOW);
            cap.setLastHealTime(0L);
            cap.setLastCombatTime(0L);
            cap.setSystemAttack(false);
        });

        // 不会自己消失//捡起物品
        mob.setPersistenceRequired();
        if (!(mob instanceof Piglin)) {
            mob.setCanPickUpLoot(false);
        }

        addHighHealthRecord(controllerUUID, mob);
    }

    /**
     * 释放对生物的控制并清理相关标记。
     *
     * @param mob 要释放的生物
     * @return {@code true} 表示该生物存在控制能力并已执行释放流程
     */
    public static boolean releaseControl(Mob mob) {
        UUID controllerUUID = getControllerUUID(mob);
        if (controllerUUID == null) {
            return false;
        }

        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        capability.ifPresent(cap -> {
            cap.setControllerUUID(null);
            cap.setControlMode(ControlMode.FOLLOW);
            cap.setLastHealTime(0L);
            cap.setLastCombatTime(0L);
            cap.setSystemAttack(false);
        });

        removeHighHealthRecord(controllerUUID, mob);
        return capability.isPresent();
    }

    private static boolean isHighHealthMob(Mob mob) {
        return mob.getMaxHealth() > Config.HIGH_HEALTH_THRESHOLD.get();
    }

    /**
     * 判断玩家是否已经控制过同类型的高生命值生物。
     *
     * @param playerUUID 玩家 UUID
     * @param mob        准备控制的目标生物
     * @return 若目标为高生命值生物且该玩家已控制同类型生物则返回 {@code true}
     */
    public static boolean hasPlayerControlledSameHighHealthMob(UUID playerUUID, Mob mob) {
        if (!isHighHealthMob(mob)) {
            return false;
        }

        if (getControlledHighHealthCount(playerUUID, mob.getType()) > 0) {
            return true;
        }

        return hasPendingHighHealthRespawn(playerUUID, mob.getType());
    }

    /**
     * 在被控制生物死亡时移除高生命值控制记录。
     *
     * @param mob 死亡生物
     */
    public static void removeControlledMobOnDeath(Mob mob) {
        UUID controllerUUID = getControllerUUID(mob);
        if (controllerUUID != null) {
            removeHighHealthRecord(controllerUUID, mob);
        }
    }

    private static void removeHighHealthRecord(UUID controllerUUID, Mob mob) {
        if (!isHighHealthMob(mob)) {
            return;
        }

        Map<EntityType<?>, Integer> controlledMobs = PLAYER_CONTROLLED_HIGH_HEALTH_MOBS.get(controllerUUID);
        if (controlledMobs != null) {
            EntityType<?> mobType = mob.getType();
            Integer currentCount = controlledMobs.get(mobType);
            if (currentCount == null) {
                return;
            }

            if (currentCount <= 1) {
                controlledMobs.remove(mobType);
            } else {
                controlledMobs.put(mobType, currentCount - 1);
            }

            if (controlledMobs.isEmpty()) {
                PLAYER_CONTROLLED_HIGH_HEALTH_MOBS.remove(controllerUUID);
            }
        }
    }

    private static void addHighHealthRecord(UUID controllerUUID, Mob mob) {
        if (!isHighHealthMob(mob)) {
            return;
        }

        PLAYER_CONTROLLED_HIGH_HEALTH_MOBS.computeIfAbsent(controllerUUID, key -> new HashMap<>())
            .merge(mob.getType(), 1, Integer::sum);
    }

    private static int getControlledHighHealthCount(UUID controllerUUID, EntityType<?> mobType) {
        Map<EntityType<?>, Integer> controlledMobs = PLAYER_CONTROLLED_HIGH_HEALTH_MOBS.get(controllerUUID);
        if (controlledMobs == null) {
            return 0;
        }
        return Math.max(0, controlledMobs.getOrDefault(mobType, 0));
    }

    private static boolean hasPendingHighHealthRespawn(UUID controllerUUID, EntityType<?> mobType) {
        for (PendingRespawnData data : PENDING_RESPAWNS.values()) {
            if (!data.controllerUUID().equals(controllerUUID) || !data.highHealthMob()) {
                continue;
            }

            Optional<EntityType<?>> pendingType = getPendingMobType(data.mobTypeId());
            if (pendingType.isPresent() && pendingType.get().equals(mobType)) {
                return true;
            }
        }
        return false;
    }

    private static Optional<EntityType<?>> getPendingMobType(String typeId) {
        ResourceLocation location = ResourceLocation.tryParse(typeId);
        if (location == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(location));
    }

    // 列表中移除[被控制的生物死亡]

    /**
     * 判断生物是否处于被控制状态。
     *
     * @param mob 生物实体
     * @return {@code true} 表示存在控制者
     */
    public static boolean isControlledEntity(LivingEntity mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        return capability.map(MobControlCapability::isControlled).orElse(false);
    }

    /**
     * 获取生物的控制者 UUID。
     *
     * @param mob 生物实体
     * @return 控制者 UUID；若未被控制则返回 {@code null}
     */
    public static @Nullable UUID getControllerUUID(LivingEntity mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        return capability.map(MobControlCapability::getControllerUUID).orElse(null);
    }

    /**
     * 在给定维度内查找生物对应的控制者玩家对象。
     *
     * @param mob   生物实体
     * @param level 查询所用世界
     * @return 控制者玩家；未找到时返回 {@code null}
     */
    @Nullable
    public static Player getController(LivingEntity mob, Level level) {
        UUID controllerUUID = getControllerUUID(mob);
        if (controllerUUID != null) {
            for (Player player : level.players()) {
                if (player.getUUID().equals(controllerUUID)) {
                    return player;
                }
            }
        }

        return null;
    }

    /**
     * 设置生物的控制模式。
     *
     * @param mob  生物实体
     * @param mode 目标控制模式
     */
    public static void setControlMode(Mob mob, ControlMode mode) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        capability.ifPresent(cap -> cap.setControlMode(mode));
    }

    /**
     * 获取生物最近一次交战时间。
     *
     * @param mob 生物实体
     * @return 最近交战的游戏时间刻；若能力缺失则返回 {@code 0L}
     */
    public static long getLastCombatTime(Mob mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        return capability.map(MobControlCapability::getLastCombatTime).orElse(0L);
    }

    /**
     * 记录生物最近一次交战时间。
     *
     * @param mob  生物实体
     * @param time 当前游戏时间刻
     */
    public static void setLastCombatTime(Mob mob, long time) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        capability.ifPresent(cap -> cap.setLastCombatTime(time));
    }

    /**
     * 以当前世界时间记录一次交战。
     *
     * @param mob 生物实体
     */
    public static void markCombat(Mob mob) {
        setLastCombatTime(mob, mob.level().getGameTime());
    }

    /**
     * 获取生物当前控制模式。
     *
     * @param mob 生物实体
     * @return 当前模式；若能力缺失则回退为 {@link ControlMode#FOLLOW}
     */
    public static ControlMode getControlMode(Mob mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        return capability.map(MobControlCapability::getControlMode).orElse(ControlMode.FOLLOW);
    }

    /**
     * 按顺序循环切换控制模式（跟随 -> 停留 -> 游荡 -> 跟随）。
     *
     * @param mob 生物实体
     * @return 切换后的新模式
     */
    public static ControlMode toggleControlMode(Mob mob) {
        ControlMode currentMode = getControlMode(mob);
        int index = currentMode.ordinal() + 1;
        ControlMode newMode = ControlMode.values()[index >= ControlMode.values().length ? 0 : index];
        setControlMode(mob, newMode);
        return newMode;
    }

    /**
     * 标记该生物当前攻击为系统触发。
     *
     * @param mob 生物实体
     */
    public static void markSystemAttack(Mob mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        capability.ifPresent(cap -> cap.setSystemAttack(true));
    }

    /**
     * 清除系统攻击标记。
     *
     * @param mob 生物实体
     */
    public static void clearSystemAttack(Mob mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        capability.ifPresent(cap -> cap.setSystemAttack(false));
    }

    /**
     * 查询系统攻击标记。
     *
     * @param mob 生物实体
     * @return {@code true} 表示当前攻击被标记为系统触发
     */
    public static boolean isSystemAttack(Mob mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        return capability.map(MobControlCapability::isSystemAttack).orElse(false);
    }

    /**
     * 为死亡生物创建延迟重生任务。
     *
     * <p>会保存实体 NBT 与能力 NBT，在 {@link #tickPendingRespawns(MinecraftServer)} 中恢复。</p>
     *
     * @param mob   死亡生物
     * @param level 当前服务端世界
     * @return {@code true} 表示成功加入待重生队列
     */
    public static boolean scheduleRespawn(Mob mob, ServerLevel level) {
        MinecraftServer server = level.getServer();
        ensurePendingRespawnsLoaded(server);

        if (mob instanceof Slime slime && !(mob instanceof MagmaCube)) {
            boolean onlyMinSize = Config.SLIME_RESPAWN_ONLY_MIN_SIZE.get();
            int slimeSize = slime.getSize();

            if (onlyMinSize ? slimeSize > 1 : slimeSize < 3) {
                return false;
            }
        }

        UUID controllerUUID = getControllerUUID(mob);
        if (controllerUUID == null || PENDING_RESPAWNS.containsKey(mob.getUUID())) {
            return false;
        }

        CompoundTag entityNbt = mob.saveWithoutId(new CompoundTag());
        entityNbt.putString("id", EntityType.getKey(mob.getType()).toString());

        CompoundTag capabilityNbt = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY)
            .map(MobControlCapability::serializeNBT)
            .orElse(new CompoundTag());

        String mobTypeId = EntityType.getKey(mob.getType()).toString();
        boolean highHealthMob = isHighHealthMob(mob);

        PENDING_RESPAWNS.put(
            mob.getUUID(), new PendingRespawnData(
                mob.getUUID(),
                controllerUUID,
                entityNbt,
                capabilityNbt,
                server.getTickCount() + Config.RESPAWN_DELAY_TICKS.get(),
                level.dimension(),
                mob.blockPosition(),
                mobTypeId,
                highHealthMob
            )
        );
        savePendingRespawns(server);
        return true;
    }

    /**
     * 每刻处理待重生队列，时间到达后尝试生成并恢复生物状态。
     *
     * @param server 当前服务端实例
     */
    public static void tickPendingRespawns(MinecraftServer server) {
        ensurePendingRespawnsLoaded(server);
        int currentTick = server.getTickCount();
        Set<UUID> completedRespawns = new HashSet<>();

        for (Map.Entry<UUID, PendingRespawnData> entry : PENDING_RESPAWNS.entrySet()) {
            PendingRespawnData data = entry.getValue();
            if (data.triggerTick() > currentTick) {
                continue;
            }

            ServerPlayer controller = server.getPlayerList().getPlayer(data.controllerUUID());
            ServerLevel targetLevel = controller != null ? controller.serverLevel() : server.getLevel(data.deathDimension());

            if (targetLevel == null) {
                continue;
            }

            CompoundTag nbt = data.entityNbt().copy();
            Optional<Entity> createdEntity = EntityType.create(nbt, targetLevel);
            if (createdEntity.isPresent() && createdEntity.get() instanceof Mob respawnedMob) {
                if (controller != null) {
                    respawnedMob.moveTo(
                        controller.getX(),
                        controller.getY(),
                        controller.getZ(),
                        respawnedMob.getYRot(),
                        respawnedMob.getXRot()
                    );
                } else {
                    respawnedMob.moveTo(
                        data.deathPos().getX() + 0.5D, data.deathPos().getY(), data.deathPos().getZ() + 0.5D,
                        respawnedMob.getYRot(), respawnedMob.getXRot()
                    );
                }

                respawnedMob.setDeltaMovement(0, 0, 0);
                respawnedMob.setHealth(respawnedMob.getMaxHealth());
                respawnedMob.setTarget(null);
                targetLevel.addFreshEntity(respawnedMob);

                addControlledMob(data.controllerUUID(), respawnedMob);
                respawnedMob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY)
                    .ifPresent(cap -> cap.deserializeNBT(data.capabilityNbt().copy()));
                clearSystemAttack(respawnedMob);

                if (controller != null) {
                    controller.sendSystemMessage(Component.translatable("mob_controller.message.respawned", respawnedMob.getDisplayName()));
                }
            }

            completedRespawns.add(entry.getKey());
        }

        if (!completedRespawns.isEmpty()) {
            for (UUID deadMobUUID : completedRespawns) {
                PENDING_RESPAWNS.remove(deadMobUUID);
            }
            savePendingRespawns(server);
        }
    }

    private static void ensurePendingRespawnsLoaded(MinecraftServer server) {
        Path filePath = getPendingRespawnFilePath(server);
        if (!filePath.equals(loadedPendingRespawnFile)) {
            loadPendingRespawns(server, filePath);
            loadedPendingRespawnFile = filePath;
        }
    }

    private static Path getPendingRespawnFilePath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT)
            .resolve("data")
            .resolve(PENDING_RESPAWN_DATA_DIR)
            .resolve(PENDING_RESPAWN_FILE);
    }

    private static void loadPendingRespawns(MinecraftServer server, Path filePath) {
        PENDING_RESPAWNS.clear();
        if (!Files.exists(filePath)) {
            return;
        }

        try (InputStream inputStream = Files.newInputStream(filePath)) {
            CompoundTag rootTag = NbtIo.readCompressed(inputStream);
            if (rootTag == null || !rootTag.contains(PENDING_RESPAWN_TAG, Tag.TAG_LIST)) {
                return;
            }

            ListTag pendingList = rootTag.getList(PENDING_RESPAWN_TAG, Tag.TAG_COMPOUND);
            for (int i = 0; i < pendingList.size(); i++) {
                CompoundTag respawnTag = pendingList.getCompound(i);
                if (!respawnTag.hasUUID("deadMobUUID") || !respawnTag.hasUUID("controllerUUID")) {
                    continue;
                }

                ResourceLocation dimensionLocation = ResourceLocation.tryParse(respawnTag.getString("deathDimension"));
                if (dimensionLocation == null) {
                    continue;
                }

                UUID deadMobUUID = respawnTag.getUUID("deadMobUUID");
                UUID controllerUUID = respawnTag.getUUID("controllerUUID");
                CompoundTag entityNbt = respawnTag.getCompound("entityNbt");
                CompoundTag capabilityNbt = respawnTag.getCompound("capabilityNbt");
                int remainingTicks = Math.max(0, respawnTag.getInt("remainingTicks"));
                ResourceKey<Level> deathDimension = ResourceKey.create(Registries.DIMENSION, dimensionLocation);
                BlockPos deathPos = BlockPos.of(respawnTag.getLong("deathPos"));

                PENDING_RESPAWNS.put(
                    deadMobUUID,
                    new PendingRespawnData(
                        deadMobUUID,
                        controllerUUID,
                        entityNbt,
                        capabilityNbt,
                        server.getTickCount() + remainingTicks,
                        deathDimension,
                        deathPos,
                        respawnTag.getString("mobTypeId"),
                        respawnTag.getBoolean("highHealthMob")
                    )
                );
            }
        } catch (IOException ignored) {
        }
    }

    private static void savePendingRespawns(MinecraftServer server) {
        Path filePath = getPendingRespawnFilePath(server);
        try {
            Files.createDirectories(filePath.getParent());

            CompoundTag rootTag = new CompoundTag();
            ListTag pendingList = new ListTag();
            int currentTick = server.getTickCount();

            for (PendingRespawnData data : PENDING_RESPAWNS.values()) {
                CompoundTag respawnTag = new CompoundTag();
                respawnTag.putUUID("deadMobUUID", data.deadMobUUID());
                respawnTag.putUUID("controllerUUID", data.controllerUUID());
                respawnTag.put("entityNbt", data.entityNbt().copy());
                respawnTag.put("capabilityNbt", data.capabilityNbt().copy());
                respawnTag.putInt("remainingTicks", Math.max(0, data.triggerTick() - currentTick));
                respawnTag.putString("deathDimension", data.deathDimension().location().toString());
                respawnTag.putLong("deathPos", data.deathPos().asLong());
                respawnTag.putString("mobTypeId", data.mobTypeId());
                respawnTag.putBoolean("highHealthMob", data.highHealthMob());
                pendingList.add(respawnTag);
            }

            rootTag.put(PENDING_RESPAWN_TAG, pendingList);

            try (OutputStream outputStream = Files.newOutputStream(filePath)) {
                NbtIo.writeCompressed(rootTag, outputStream);
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * 控制模式。
     */
    public enum ControlMode {
        /**
         * 跟随
         */
        FOLLOW,
        /**
         * 停留
         */
        STAY,
        /**
         * 游荡
         */
        WANDER,
    }

    private record PendingRespawnData(
        UUID deadMobUUID, UUID controllerUUID, CompoundTag entityNbt,
        CompoundTag capabilityNbt, int triggerTick,
        ResourceKey<Level> deathDimension,
        BlockPos deathPos,
        String mobTypeId,
        boolean highHealthMob
    ) {
    }
}
