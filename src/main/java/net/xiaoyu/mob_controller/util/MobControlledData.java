package net.xiaoyu.mob_controller.util;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.util.LazyOptional;
import net.xiaoyu.mob_controller.capability.*;

import java.util.*;
import java.util.concurrent.*;

public class MobControlledData {
    private static final Map<UUID, Set<EntityType<?>>> playerControlledHighHealthMobs = new ConcurrentHashMap<>();
    public static final int HIGH_HEALTH_THRESHOLD = 150;
    
    public enum ControlMode {
        FOLLOW, // 跟随
        STAY, // 停留
    }
    
    public static void addControlledMob(UUID controllerUUID, Mob mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        capability.ifPresent(cap -> {
            cap.setControllerUUID(controllerUUID);
            cap.setControlMode(ControlMode.FOLLOW);
        });

        // 不会自己消失//捡起物品
        mob.setPersistenceRequired();
        mob.setCanPickUpLoot(false);

        if (isHighHealthMob(mob)) {
            playerControlledHighHealthMobs.computeIfAbsent(controllerUUID, k -> new HashSet<>()).add(mob.getType());
        }
    }

    private static boolean isHighHealthMob(Mob mob) {
        return mob.getMaxHealth() > HIGH_HEALTH_THRESHOLD;
    }

    public static boolean hasPlayerControlledSameHighHealthMob(UUID playerUUID, Mob mob) {
        if (!isHighHealthMob(mob)) {
            return false;
        }
        
        Set<EntityType<?>> controlledMobs = playerControlledHighHealthMobs.get(playerUUID);
        return controlledMobs != null && controlledMobs.contains(mob.getType());
    }
    
    // 列表中移除[被控制的生物死亡]
    public static void removeControlledMobOnDeath(Mob mob) {
        UUID controllerUUID = getControllerUUID(mob);
        if (controllerUUID != null && isHighHealthMob(mob)) {
            Set<EntityType<?>> controlledMobs = playerControlledHighHealthMobs.get(controllerUUID);
            if (controlledMobs != null) {
                controlledMobs.remove(mob.getType());
                if (controlledMobs.isEmpty()) {
                    playerControlledHighHealthMobs.remove(controllerUUID);
                }
            }
        }
    }
    
    public static boolean isControlledMob(Mob mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        return capability.map(MobControlCapability::isControlled).orElse(false);
    }
    
    public static UUID getControllerUUID(Mob mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        return capability.map(cap -> {
            UUID uuid = cap.getControllerUUID();
            return uuid;
        }).orElse(null);
    }
    
    public static Player getController(Mob mob, Level level) {
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
    
    public static boolean isControlledEntity(LivingEntity entity) {
        if (entity instanceof Mob) {
            return isControlledMob((Mob) entity);
        }
        
        return false;
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
        ControlMode newMode = (currentMode == ControlMode.FOLLOW) ? ControlMode.STAY : ControlMode.FOLLOW;
        setControlMode(mob, newMode);
        return newMode;
    }

    private static final ThreadLocal<Mob> systemAttackMarker = new ThreadLocal<>();
    
    public static void markSystemAttack(Mob mob) {
        systemAttackMarker.set(mob);
    }
    
    public static void clearSystemAttack(Mob mob) {
        if (systemAttackMarker.get() == mob) {
            systemAttackMarker.remove();
        }
    }
    
    public static boolean isSystemAttack(Mob mob) {
        return systemAttackMarker.get() == mob;
    }
}