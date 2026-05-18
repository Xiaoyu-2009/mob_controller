package net.xiaoyu.mob_controller.mixin.cataclysm;

import com.github.L_Ender.cataclysm.entity.InternalAnimationMonster.IABossMonsters.Maledictus.Maledictus_Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(Maledictus_Entity.class)
public class MixinMaledictusNoRespawner {

    @Inject(method = "AfterDefeatBoss", at = @At("HEAD"), cancellable = true, remap = false)
    private void onAfterDefeatBoss(@Nullable LivingEntity living, CallbackInfo ci) {
        Object self = this;
        if (Config.ENABLE_RESPAWN.get() && self instanceof Mob mob && MobControlledData.isControlledEntity(mob)) {
            ci.cancel();
        }
    }
}