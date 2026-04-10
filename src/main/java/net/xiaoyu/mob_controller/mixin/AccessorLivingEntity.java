package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/**
 * 生物实体私有字段访问器。
 *
 * <p>用于读取跳跃状态与缓降属性修饰器常量。</p>
 */
@SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
@Mixin(LivingEntity.class)
public interface AccessorLivingEntity {
    /**
     * 获取跳跃标记。
     */
    @Accessor("jumping")
    boolean mob_controller$getJumping();

    /**
     * 获取缓降属性修饰器常量。
     */
    @Accessor(value = "SLOW_FALLING", remap = false)
    static AttributeModifier mob_controller$getSlowFalling() {
        return null;
    }
}
