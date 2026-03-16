package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
@Mixin(LivingEntity.class)
public interface AccessorLivingEntity {
    @Accessor("jumping")
    boolean mob_controller$getJumping();

    @Accessor(value = "SLOW_FALLING", remap = false)
    static AttributeModifier mob_controller$getSlowFalling() {
        return null;
    }
}
