package net.xiaoyu.mob_controller.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
@Mixin(targets = "net.minecraft.world.entity.monster.Slime$SlimeMoveControl")
public interface AccessorSlimeMoveControl {
    @Invoker("setDirection")
    void mob_controller$setDirection(float yRot, boolean aggressive);
}
