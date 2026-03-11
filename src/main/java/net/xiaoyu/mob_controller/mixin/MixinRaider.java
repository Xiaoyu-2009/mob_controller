package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Raider.class)
public abstract class MixinRaider extends PatrollingMonster {
    protected MixinRaider(EntityType<? extends PatrollingMonster> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "getCurrentRaid()Lnet/minecraft/world/entity/raid/Raid;", at = @At("RETURN"), cancellable = true)
    private void injectGetCurrentRaid(CallbackInfoReturnable<Raid> cir) {
        if (MobControlledData.isControlledMob(this)) {
            cir.setReturnValue(null);
        }
    }
}
