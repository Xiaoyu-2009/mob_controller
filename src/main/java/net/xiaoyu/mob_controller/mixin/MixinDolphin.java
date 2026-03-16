package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Dolphin.class)
public abstract class MixinDolphin extends WaterAnimal {
    protected MixinDolphin(EntityType<? extends WaterAnimal> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), cancellable = true)
    private void injectTravel(Vec3 travelVector, CallbackInfo ci) {
        if (this.getFirstPassenger() != null) {
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
            ci.cancel();
        }
    }

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void injectTick(CallbackInfo ci) {
        if (this.getFirstPassenger() != null) {
            if (!this.isInWaterOrBubble()) {
                Vec3 deltaMovement = this.getDeltaMovement().scale(0.9D);
                double d0 = 0.08D;
                AttributeInstance gravity = this.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
                if (gravity != null) {
                    boolean flag = this.getDeltaMovement().y <= 0.0D;
                    AttributeModifier slowFalling = AccessorLivingEntity.mob_controller$getSlowFalling();
                    if (slowFalling != null) {
                        if (flag && this.hasEffect(MobEffects.SLOW_FALLING)) {
                            if (!gravity.hasModifier(slowFalling)) {
                                gravity.addTransientModifier(slowFalling);
                            }
                        } else if (gravity.hasModifier(slowFalling)) {
                            gravity.removeModifier(slowFalling);
                        }
                    }
                    d0 = gravity.getValue();
                }
                this.setDeltaMovement(deltaMovement.add(0, -d0, 0));
            }
        }
    }
}
