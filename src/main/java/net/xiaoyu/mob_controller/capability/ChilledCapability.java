package net.xiaoyu.mob_controller.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public class ChilledCapability implements INBTSerializable<CompoundTag> {
    private boolean chilled = false;

    public boolean isChilled() {
        return chilled;
    }

    public void setChilled(boolean chilled) {
        this.chilled = chilled;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Chilled", chilled);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.chilled = nbt.getBoolean("Chilled");
    }
}