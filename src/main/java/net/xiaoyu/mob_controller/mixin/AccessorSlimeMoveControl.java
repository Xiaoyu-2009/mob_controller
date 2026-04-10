package net.xiaoyu.mob_controller.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
/**
 * 史莱姆移动控制私有方法访问器。
 */

@SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
@Mixin(targets = "net.minecraft.world.entity.monster.Slime$SlimeMoveControl")
public interface AccessorSlimeMoveControl {
    /**
     * 调用原始 {@code setDirection} 方法。
     */
    @Invoker("setDirection")
    void mob_controller$setDirection(float yRot, boolean aggressive);
}
