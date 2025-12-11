package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(ForgeEventFactory.class)
public class ForgeEventFactoryMixin {

    @Inject(method = "getMobGriefingEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onGetMobGriefingEvent(Level level, @Nullable Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Mob) {
            Mob mob = (Mob) entity;
            
            if (MobControlledData.isControlledMob(mob)) {
                cir.setReturnValue(false);
            }
        } else if (entity instanceof Projectile) {
            Projectile projectile = (Projectile) entity;
            Entity owner = projectile.getOwner();
            
            if (owner instanceof Mob && MobControlledData.isControlledMob((Mob) owner)) {
                cir.setReturnValue(false);
            }
        }
    }
}