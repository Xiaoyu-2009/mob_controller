package net.xiaoyu.mob_controller.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;

public class MobControlCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    @SuppressWarnings("NotNullFieldNotInitialized")
    public static Capability<MobControlCapability> MOB_CONTROL_CAPABILITY;

    @Nullable
    private MobControlCapability capability = null;
    private final LazyOptional<MobControlCapability> lazyCapability = LazyOptional.of(this::createCapability);

    private MobControlCapability createCapability() {
        if (capability == null) {
            capability = new MobControlCapability();
        }

        return capability;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap.equals(MOB_CONTROL_CAPABILITY)) {
            return lazyCapability.cast();
        }

        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return createCapability().serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createCapability().deserializeNBT(nbt);
    }
}