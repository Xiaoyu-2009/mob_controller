package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;
import java.util.Optional;

@Mixin(Warden.class)
public class WardenMixin {
    @Unique
    private static final int CONTROLLED_WARDEN_DARKNESS_DURATION = 260;
    @Unique
    private static final int CONTROLLED_WARDEN_DARKNESS_REFRESH_MARGIN = 40;

    @Inject(method = "canTargetEntity", at = @At("HEAD"), cancellable = true)
    private void targetWarden(@Nullable Entity entity, CallbackInfoReturnable<Boolean> info) {
        Warden warden = (Warden) (Object) this;

        if (entity instanceof LivingEntity livingEntity) {

            // 被控制的坚守者取消攻击欲望
            if (MobControlledData.isControlledEntity(warden)) {
                if (!MobControlUtil.isEnemy(warden, livingEntity)) {
                    info.cancel();
                }
            }
        }
    }

    @Inject(method = "customServerAiStep()V", at = @At("RETURN"))
    private void injectControlledWardenDarkness(CallbackInfo ci) {
        Warden warden = (Warden) (Object) this;

        if (!MobControlledData.isControlledEntity(warden) || (warden.tickCount + warden.getId()) % 20 != 0) {
            return;
        }

        Optional<LivingEntity> angryTarget = warden.getEntityAngryAt();
        LivingEntity livingTarget = warden.getTarget();
        if (angryTarget.isEmpty() && livingTarget != null) {
            angryTarget = Optional.of(livingTarget);
        }

        if (angryTarget.isPresent() && angryTarget.get() instanceof Player player && MobControlUtil.isEnemy(warden, player)) {
            MobEffectInstance darkness = new MobEffectInstance(MobEffects.DARKNESS, CONTROLLED_WARDEN_DARKNESS_DURATION, 0, false, false);
            if (!player.hasEffect(MobEffects.DARKNESS)
                    || Objects.requireNonNull(player.getEffect(MobEffects.DARKNESS)).endsWithin(CONTROLLED_WARDEN_DARKNESS_REFRESH_MARGIN)) {
                player.addEffect(darkness, warden);
            }
        }
    }
}