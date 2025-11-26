package net.xiaoyu.mob_controller.capability;

import net.minecraft.nbt.CompoundTag;
import net.xiaoyu.mob_controller.util.MobControlledData;

import java.util.*;

public class MobControlCapability {
    private UUID controllerUUID = null;
    private MobControlledData.ControlMode controlMode = MobControlledData.ControlMode.FOLLOW;
    private long lastHealTime = 0;
    
    public MobControlCapability() {}

    public UUID getControllerUUID() {
        return controllerUUID;
    }

    public void setControllerUUID(UUID uuid) {
        this.controllerUUID = uuid;
    }
    
    public MobControlledData.ControlMode getControlMode() {
        return controlMode;
    }

    public void setControlMode(MobControlledData.ControlMode mode) {
        this.controlMode = mode;
    }
    
    public boolean isControlled() {
        return controllerUUID != null;
    }

    public long getLastHealTime() {
        return lastHealTime;
    }

    public void setLastHealTime(long time) {
        this.lastHealTime = time;
    }
    
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        if (controllerUUID != null) {
            nbt.putUUID("ControllerUUID", controllerUUID);
        }
        nbt.putString("ControlMode", controlMode.name());
        nbt.putLong("LastHealTime", lastHealTime);
        return nbt;
    }
    
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("ControllerUUID")) {
            controllerUUID = nbt.getUUID("ControllerUUID");
        } else {
            controllerUUID = null;
        }

        try {
            controlMode = MobControlledData.ControlMode.valueOf(nbt.getString("ControlMode"));
        } catch (IllegalArgumentException e) {
            controlMode = MobControlledData.ControlMode.FOLLOW;
        }

        lastHealTime = nbt.getLong("LastHealTime");
    }
}