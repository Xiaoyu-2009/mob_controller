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

public class MobControlledData {
    private static final Map<UUID, Set<EntityType<?>>> PLAYER_CONTROLLED_HIGH_HEALTH_MOBS = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingRespawnData> PENDING_RESPAWNS = new ConcurrentHashMap<>();
    public static final int HIGH_HEALTH_THRESHOLD = 150;
    public static final int RESPAWN_DELAY_TICKS = 600;

    private record PendingRespawnData(UUID deadMobUUID, UUID controllerUUID, CompoundTag entityNbt,
                                      CompoundTag capabilityNbt, int triggerTick,
                                      net.minecraft.resources.ResourceKey<Level> deathDimension,
                                      BlockPos deathPos) {
    }

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

    public static boolean hasPlayerControlledSameHighHealthMob(UUID playerUUID, Mob mob) {
        if (!isHighHealthMob(mob)) {
            return false;
        }

        Set<EntityType<?>> controlledMobs = PLAYER_CONTROLLED_HIGH_HEALTH_MOBS.get(playerUUID);
        return controlledMobs != null && controlledMobs.contains(mob.getType());
    }

    // 列表中移除[被控制的生物死亡]
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

    public static boolean isControlledEntity(LivingEntity mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        return capability.map(MobControlCapability::isControlled).orElse(false);
    }

    public static @Nullable UUID getControllerUUID(LivingEntity mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        return capability.map(MobControlCapability::getControllerUUID).orElse(null);
    }

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

    public static void setControlMode(Mob mob, ControlMode mode) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        capability.ifPresent(cap -> cap.setControlMode(mode));
    }

    public static ControlMode getControlMode(Mob mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        return capability.map(MobControlCapability::getControlMode).orElse(ControlMode.FOLLOW);
    }

    public static ControlMode toggleControlMode(Mob mob) {
        ControlMode currentMode = getControlMode(mob);
        int index = currentMode.ordinal() + 1;
        ControlMode newMode = ControlMode.values()[index >= ControlMode.values().length ? 0 : index];
        setControlMode(mob, newMode);
        return newMode;
    }

    public static void markSystemAttack(Mob mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        capability.ifPresent(cap -> cap.setSystemAttack(true));
    }

    public static void clearSystemAttack(Mob mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        capability.ifPresent(cap -> cap.setSystemAttack(false));
    }

    public static boolean isSystemAttack(Mob mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        return capability.map(MobControlCapability::isSystemAttack).orElse(false);
    }

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