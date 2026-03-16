package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Guardian.class)
public abstract class MixinGuardian extends Monster {
    protected MixinGuardian(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "isMoving()Z", at = @At("RETURN"), cancellable = true)
    private void injectIsMoving(CallbackInfoReturnable<Boolean> cir) {
        if (this.getControllingPassenger() != null) {
            cir.setReturnValue(true);
        }
    }
}
