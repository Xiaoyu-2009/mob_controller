package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
@Mixin(LivingEntity.class)
public interface AccessorLivingEntity {
    @Accessor("jumping")
    boolean mob_controller$getJumping();
}
