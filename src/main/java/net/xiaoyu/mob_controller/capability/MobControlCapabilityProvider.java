package net.xiaoyu.mob_controller.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;

/**
 * {@link MobControlCapability} 的 Forge Capability 提供者，负责将能力实例附加到实体上。
 *
 * <p>该类实现 {@link ICapabilityProvider} 与 {@link INBTSerializable}，
 * 由 Forge 在 {@code AttachCapabilitiesEvent} 中由事件监听器实例化并挂载到每个 {@link net.minecraft.world.entity.Mob}。</p>
 *
 * <p>能力实例通过 {@link LazyOptional} 延迟初始化，仅在首次调用 {@link #getCapability} 时创建。</p>
 *
 * @see MobControlCapability
 * @see MobControlCapabilityRegister
 */
public class MobControlCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    /**
     * 本模组用于查询控制能力的 Capability 令牌，在 {@link MobControlCapabilityRegister#registerCapabilities} 中赋值。
     *
     * <p>该字段必须在使用前已被赋值（即模组能力注册事件已触发），
     * 否则 {@code getCapability} 将始终返回 {@link LazyOptional#empty()}。</p>
     */
    @SuppressWarnings("NotNullFieldNotInitialized")
    public static Capability<MobControlCapability> MOB_CONTROL_CAPABILITY;

    @Nullable
    private MobControlCapability capability = null;
    private final LazyOptional<MobControlCapability> lazyCapability = LazyOptional.of(this::createCapability);

    /**
     * 惰性创建 {@link MobControlCapability} 实例。
     *
     * <p>若实例尚未创建则新建，否则直接返回已有实例。</p>
     *
     * @return 非 null 的能力实例
     */
    private MobControlCapability createCapability() {
        if (capability == null) {
            capability = new MobControlCapability();
        }
        return capability;
    }

    /**
     * 向 Forge 能力系统提供本能力实例。
     *
     * <p>仅当 {@code cap} 等于 {@link #MOB_CONTROL_CAPABILITY} 时返回有效的
     * {@link LazyOptional}，其他键均返回 {@link LazyOptional#empty()}。</p>
     *
     * @param cap  请求的能力类型令牌
     * @param side 查询面向（对实体能力通常忽略此参数）
     * @param <T>  能力接口类型
     * @return 若匹配则返回包含本能力实例的 {@link LazyOptional}，否则返回空
     */
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap.equals(MOB_CONTROL_CAPABILITY)) {
            return lazyCapability.cast();
        }
        return LazyOptional.empty();
    }

    /**
     * 将能力数据序列化为 NBT，由 Forge 在实体存档时调用。
     *
     * @return 包含控制能力数据的 {@link CompoundTag}
     */
    @Override
    public CompoundTag serializeNBT() {
        return createCapability().serializeNBT();
    }

    /**
     * 从 NBT 中反序列化能力数据，由 Forge 在实体加载时调用。
     *
     * @param nbt 来自存档或同步包的 {@link CompoundTag}
     */
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createCapability().deserializeNBT(nbt);
    }
}
