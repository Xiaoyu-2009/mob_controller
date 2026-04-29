package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Phantom.class)
public interface PhantomAccessor {
    @Accessor("moveTargetPoint")
    void setMoveTargetPoint(Vec3 point);
}