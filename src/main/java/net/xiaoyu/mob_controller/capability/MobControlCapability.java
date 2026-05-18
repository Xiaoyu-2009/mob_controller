package net.xiaoyu.mob_controller.capability;

import net.minecraft.nbt.CompoundTag;
import net.xiaoyu.mob_controller.util.MobControlledData;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 存储单个生物的控制状态数据，作为 Forge Capability 附加到每个 {@link net.minecraft.world.entity.Mob} 实体上。
 *
 * <p>该类保存的字段包括：</p>
 * <ul>
 *   <li><b>controllerUUID</b>：控制者（玩家）的 UUID，{@code null} 表示该生物当前未被控制；</li>
 *   <li><b>controlMode</b>：当前控制模式（跟随 / 停留 / 游荡）；</li>
 *   <li><b>lastHealTime</b>：上一次被治愈的游戏时间刻，用于冷却计算；</li>
 *   <li><b>lastCombatTime</b>：最近一次进入战斗/发生交战的游戏时间刻，用于脱战判定；</li>
 *   <li><b>isSystemAttack</b>：标记当前攻击是否由系统（非玩家手动指令）发起，用于区分仇恨源头；</li>
 *   <li><b>aggressiveMode</b>：索敌模式标记，{@code true} 表示生物会主动攻击敌对生物；</li>
 *   <li><b>legionMode</b>：军团模式标记，{@code true} 表示该生物参与军团战斗；</li>
 *   <li><b>previousAggressiveMode</b>：进入军团模式前记忆的索敌模式状态，用于退出时恢复；</li>
 *   <li><b>isSummoned</b>：是否为召唤物（如恼鬼），影响重生逻辑；</li>
 *   <li><b>splitOffspring</b>：是否为分裂产生的子个体（如史莱姆），影响继承逻辑。</li>
 * </ul>
 *
 * <p>注意：军团模式的队伍颜色不再存储于能力中，而是在渲染时动态从控制者玩家的持久数据中获取，
 * 这样切换颜色后能立即生效，无需遍历所有生物，且对未加载实体友好。</p>
 */
public class MobControlCapability {
    @Nullable
    private UUID controllerUUID = null;
    private MobControlledData.ControlMode controlMode = MobControlledData.ControlMode.FOLLOW;
    private long lastHealTime = 0;
    private long lastCombatTime = 0;
    private boolean isSystemAttack = false;
    private boolean aggressiveMode = false;
    private boolean legionMode = false;
    private boolean previousAggressiveMode = false;
    private boolean isSummoned = false;
    private boolean splitOffspring = false;

    public MobControlCapability() {
    }

    // ========== Getters / Setters ==========
    @Nullable
    public UUID getControllerUUID() {
        return controllerUUID;
    }

    public void setControllerUUID(@Nullable UUID uuid) {
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

    public long getLastCombatTime() {
        return lastCombatTime;
    }

    public void setLastCombatTime(long time) {
        this.lastCombatTime = time;
    }

    public boolean isSystemAttack() {
        return isSystemAttack;
    }

    public void setSystemAttack(boolean systemAttack) {
        isSystemAttack = systemAttack;
    }

    public boolean isAggressiveMode() {
        return aggressiveMode;
    }

    public void setAggressiveMode(boolean aggressiveMode) {
        this.aggressiveMode = aggressiveMode;
    }

    public boolean isLegionMode() {
        return legionMode;
    }

    public void setLegionMode(boolean mode) {
        if (this.legionMode == mode) return;
        if (mode) {
            // 进入军团模式：保存当前的 aggression 状态，并强制设为 false（护主）
            this.previousAggressiveMode = this.aggressiveMode;
            this.aggressiveMode = false;
        } else {
            // 退出军团模式：恢复之前的 aggression 状态
            this.aggressiveMode = this.previousAggressiveMode;
            this.previousAggressiveMode = false;
        }
        this.legionMode = mode;
    }

    public boolean isSummoned() {
        return isSummoned;
    }

    public void setSummoned(boolean summoned) {
        isSummoned = summoned;
    }

    public boolean isSplitOffspring() {
        return splitOffspring;
    }

    public void setSplitOffspring(boolean splitOffspring) {
        this.splitOffspring = splitOffspring;
    }

    // ========== NBT 序列化 ==========
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        if (controllerUUID != null) {
            nbt.putUUID("ControllerUUID", controllerUUID);
        }
        nbt.putString("ControlMode", controlMode.name());
        nbt.putLong("LastHealTime", lastHealTime);
        nbt.putLong("LastCombatTime", lastCombatTime);
        nbt.putBoolean("IsSystemAttack", isSystemAttack);
        nbt.putBoolean("AggressiveMode", aggressiveMode);
        nbt.putBoolean("IsSummoned", isSummoned);
        nbt.putBoolean("SplitOffspring", splitOffspring);
        nbt.putBoolean("LegionMode", legionMode);
        nbt.putBoolean("PreviousAggressiveMode", previousAggressiveMode);
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
        lastCombatTime = nbt.getLong("LastCombatTime");
        isSystemAttack = nbt.getBoolean("IsSystemAttack");
        aggressiveMode = nbt.getBoolean("AggressiveMode");
        isSummoned = nbt.getBoolean("IsSummoned");
        splitOffspring = nbt.getBoolean("SplitOffspring");
        legionMode = nbt.getBoolean("LegionMode");
        previousAggressiveMode = nbt.getBoolean("PreviousAggressiveMode");
    }
}