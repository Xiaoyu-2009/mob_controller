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
 *   <li><b>isSystemAttack</b>：标记当前攻击是否由系统（非玩家手动指令）发起，
 *       用于区分仇恨源头。</li>
 * </ul>
 *
 * <p>实例由 {@link MobControlCapabilityProvider} 延迟创建，
 * 并通过 {@link MobControlCapabilityProvider#MOB_CONTROL_CAPABILITY} 键访问。
 * 数据通过 {@link #serializeNBT()} / {@link #deserializeNBT(CompoundTag)} 持久化。</p>
 *
 * @see MobControlCapabilityProvider
 * @see MobControlledData
 */
public class MobControlCapability {
    @Nullable
    private UUID controllerUUID = null;
    private MobControlledData.ControlMode controlMode = MobControlledData.ControlMode.FOLLOW;
    private long lastHealTime = 0;
    private boolean isSystemAttack = false;

    /** 无参构造器，所有字段使用默认值（未控制、跟随模式）。 */
    public MobControlCapability() {
    }

    /**
     * 获取控制该生物的玩家 UUID。
     *
     * @return 控制者的 {@link UUID}；若生物未被控制则返回 {@code null}
     */
    @Nullable
    public UUID getControllerUUID() {
        return controllerUUID;
    }

    /**
     * 设置控制该生物的玩家 UUID。
     *
     * <p>传入 {@code null} 时等同于释放控制。</p>
     *
     * @param uuid 控制者的 UUID，或 {@code null} 以清除控制状态
     */
    public void setControllerUUID(UUID uuid) {
        this.controllerUUID = uuid;
    }

    /**
     * 获取当前控制模式。
     *
     * @return {@link MobControlledData.ControlMode} 枚举值，默认为 {@link MobControlledData.ControlMode#FOLLOW}
     */
    public MobControlledData.ControlMode getControlMode() {
        return controlMode;
    }

    /**
     * 设置当前控制模式。
     *
     * @param mode 新的控制模式，不可为 {@code null}
     */
    public void setControlMode(MobControlledData.ControlMode mode) {
        this.controlMode = mode;
    }

    /**
     * 判断生物当前是否处于被控制状态。
     *
     * <p>等价于 {@code getControllerUUID() != null}。</p>
     *
     * @return {@code true} 表示已被某玩家控制
     */
    public boolean isControlled() {
        return controllerUUID != null;
    }

    /**
     * 获取上次治愈的游戏时间刻（game tick），用于治愈冷却判断。
     *
     * @return 上次治愈时的 {@link net.minecraft.world.level.Level#getGameTime()} 值；
     *         默认为 {@code 0L}（从未被治愈）
     */
    public long getLastHealTime() {
        return lastHealTime;
    }

    /**
     * 更新上次治愈的游戏时间刻。
     *
     * @param time 当前的 {@link net.minecraft.world.level.Level#getGameTime()} 值
     */
    public void setLastHealTime(long time) {
        this.lastHealTime = time;
    }

    /**
     * 判断当前攻击是否为系统发起（而非玩家手动指令触发）。
     *
     * <p>系统攻击标记用于在生物死亡时判断是否触发重生逻辑：
     * 若为系统攻击（即由跟随保护机制触发的攻击）导致生物死亡，则不重生；
     * 否则安排延迟重生。</p>
     *
     * @return {@code true} 表示当前攻击由系统发起
     */
    public boolean isSystemAttack() {
        return isSystemAttack;
    }

    /**
     * 设置当前攻击是否为系统发起。
     *
     * @param systemAttack {@code true} 表示标记为系统攻击，{@code false} 则清除标记
     */
    public void setSystemAttack(boolean systemAttack) {
        isSystemAttack = systemAttack;
    }

    /**
     * 将能力数据序列化为 {@link CompoundTag} 以写入磁盘或通过网络同步。
     *
     * <p>写入的键名如下：</p>
     * <ul>
     *   <li>{@code "ControllerUUID"}（仅在非 {@code null} 时写入）</li>
     *   <li>{@code "ControlMode"}</li>
     *   <li>{@code "LastHealTime"}</li>
     *   <li>{@code "IsSystemAttack"}</li>
     * </ul>
     *
     * @return 包含本能力数据的 NBT 标签
     */
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        if (controllerUUID != null) {
            nbt.putUUID("ControllerUUID", controllerUUID);
        }
        nbt.putString("ControlMode", controlMode.name());
        nbt.putLong("LastHealTime", lastHealTime);
        nbt.putBoolean("IsSystemAttack", isSystemAttack);
        return nbt;
    }

    /**
     * 从 {@link CompoundTag} 中反序列化能力数据，用于从磁盘读取或接收网络同步包时还原状态。
     *
     * <p>若 {@code "ControlMode"} 字段值无效（例如旧版本遗留数据），则回退到
     * {@link MobControlledData.ControlMode#FOLLOW}。</p>
     *
     * @param nbt 含有能力数据的 NBT 标签，通常来自 {@link #serializeNBT()} 的输出
     */
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
        isSystemAttack = nbt.getBoolean("IsSystemAttack");
    }
}
