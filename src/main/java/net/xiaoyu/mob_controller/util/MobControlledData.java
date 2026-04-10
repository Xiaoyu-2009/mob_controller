package net.xiaoyu.mob_controller.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import net.xiaoyu.mob_controller.capability.MobControlCapability;
import net.xiaoyu.mob_controller.capability.MobControlCapabilityProvider;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
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
    /** 玩家 -> 已控制的高生命值生物类型集合。 */
    private static final Map<UUID, Set<EntityType<?>>> PLAYER_CONTROLLED_HIGH_HEALTH_MOBS = new ConcurrentHashMap<>();
    /** 待执行的延迟重生任务。键为死亡生物 UUID。 */
    private static final Map<UUID, PendingRespawnData> PENDING_RESPAWNS = new ConcurrentHashMap<>();
    /** 判定为“高生命值生物”的生命值阈值。 */
    public static final int HIGH_HEALTH_THRESHOLD = 150;
    /** 生物死亡后触发重生的延迟刻数（600 tick = 30 秒）。 */
    public static final int RESPAWN_DELAY_TICKS = 600;

    private record PendingRespawnData(UUID deadMobUUID, UUID controllerUUID, CompoundTag entityNbt,
                                      CompoundTag capabilityNbt, int triggerTick,
                                      net.minecraft.resources.ResourceKey<Level> deathDimension,
                                      BlockPos deathPos) {
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
        });

        // 不会自己消失//捡起物品
        mob.setPersistenceRequired();
        if (!(mob instanceof Piglin)) {
            mob.setCanPickUpLoot(false);
        }

        if (isHighHealthMob(mob)) {
            PLAYER_CONTROLLED_HIGH_HEALTH_MOBS.computeIfAbsent(controllerUUID, k -> new HashSet<>()).add(mob.getType());
        }
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
            cap.setSystemAttack(false);
        });

        removeHighHealthRecord(controllerUUID, mob);
        return capability.isPresent();
    }

    private static boolean isHighHealthMob(Mob mob) {
        return mob.getMaxHealth() > HIGH_HEALTH_THRESHOLD;
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

        Set<EntityType<?>> controlledMobs = PLAYER_CONTROLLED_HIGH_HEALTH_MOBS.get(playerUUID);
        return controlledMobs != null && controlledMobs.contains(mob.getType());
    }

    // 列表中移除[被控制的生物死亡]
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

        Set<EntityType<?>> controlledMobs = PLAYER_CONTROLLED_HIGH_HEALTH_MOBS.get(controllerUUID);
        if (controlledMobs != null) {
            controlledMobs.remove(mob.getType());
            if (controlledMobs.isEmpty()) {
                PLAYER_CONTROLLED_HIGH_HEALTH_MOBS.remove(controllerUUID);
            }
        }
    }

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
        UUID controllerUUID = getControllerUUID(mob);
        if (controllerUUID == null || PENDING_RESPAWNS.containsKey(mob.getUUID())) {
            return false;
        }

        CompoundTag entityNbt = mob.saveWithoutId(new CompoundTag());
        entityNbt.putString("id", EntityType.getKey(mob.getType()).toString());

        CompoundTag capabilityNbt = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY)
                .map(MobControlCapability::serializeNBT)
                .orElse(new CompoundTag());

        PENDING_RESPAWNS.put(mob.getUUID(), new PendingRespawnData(
                mob.getUUID(),
                controllerUUID,
                entityNbt,
                capabilityNbt,
                level.getServer().getTickCount() + RESPAWN_DELAY_TICKS,
                level.dimension(),
                mob.blockPosition()
        ));
        return true;
    }

    /**
     * 每刻处理待重生队列，时间到达后尝试生成并恢复生物状态。
     *
     * @param server 当前服务端实例
     */
    public static void tickPendingRespawns(MinecraftServer server) {
        int currentTick = server.getTickCount();

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
            java.util.Optional<Entity> createdEntity = EntityType.create(nbt, targetLevel);
            if (createdEntity.isPresent() && createdEntity.get() instanceof Mob respawnedMob) {
                if (controller != null) {
                    respawnedMob.moveTo(controller.getX(), controller.getY(), controller.getZ(), respawnedMob.getYRot(), respawnedMob.getXRot());
                } else {
                    respawnedMob.moveTo(data.deathPos().getX() + 0.5D, data.deathPos().getY(), data.deathPos().getZ() + 0.5D,
                            respawnedMob.getYRot(), respawnedMob.getXRot());
                }

                respawnedMob.setDeltaMovement(0, 0, 0);
                respawnedMob.setHealth(respawnedMob.getMaxHealth());
                respawnedMob.setTarget(null);
                targetLevel.addFreshEntity(respawnedMob);

                addControlledMob(data.controllerUUID(), respawnedMob);
                respawnedMob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY)
                        .ifPresent(cap -> cap.deserializeNBT(data.capabilityNbt().copy()));
                clearSystemAttack(respawnedMob);
            }

            PENDING_RESPAWNS.remove(entry.getKey());
        }
    }
}
