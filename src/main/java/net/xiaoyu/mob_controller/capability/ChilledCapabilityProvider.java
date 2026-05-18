package net.xiaoyu.mob_controller.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChilledCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final Capability<ChilledCapability> CHILLED_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    private ChilledCapability capability = null;
    private final LazyOptional<ChilledCapability> lazyOptional = LazyOptional.of(this::getOrCreateCapability);

    private ChilledCapability getOrCreateCapability() {
        if (capability == null) {
            capability = new ChilledCapability();
        }
        return capability;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == CHILLED_CAPABILITY ? lazyOptional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return getOrCreateCapability().serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        getOrCreateCapability().deserializeNBT(nbt);
    }
}