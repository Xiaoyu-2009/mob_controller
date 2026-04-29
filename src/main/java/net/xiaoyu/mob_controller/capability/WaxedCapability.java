package net.xiaoyu.mob_controller.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public class WaxedCapability implements INBTSerializable<CompoundTag> {
    private boolean waxed = false;

    public boolean isWaxed() {
        return waxed;
    }

    public void setWaxed(boolean waxed) {
        this.waxed = waxed;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Waxed", waxed);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.waxed = nbt.getBoolean("Waxed");
    }
}