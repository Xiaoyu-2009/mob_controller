package net.xiaoyu.mob_controller.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.*;

import javax.annotation.*;

public class MobControlCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static Capability<MobControlCapability> MOB_CONTROL_CAPABILITY = null;
    
    private MobControlCapability capability = null;
    private final LazyOptional<MobControlCapability> lazyCapability = LazyOptional.of(this::createCapability);
    
    private MobControlCapability createCapability() {
        if (capability == null) {
            capability = new MobControlCapability();
        }

        return capability;
    }
    
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == MOB_CONTROL_CAPABILITY) {
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