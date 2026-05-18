package net.xiaoyu.mob_controller.mixin;

import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.level.Level;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

/**
 * 远古守卫者行为注入。
 *
 * <p>受控远古守卫者在服务端 AI 步骤中对敌对玩家维持挖掘疲劳效果。</p>
 */
@Mixin(ElderGuardian.class)
public abstract class MixinElderGuardian extends Guardian {
    /**
     * 挖掘疲劳持续时间。
     */
    @Unique
    private static final int CONTROLLED_ELDER_GUARDIAN_EFFECT_DURATION = 6000;
    /**
     * 挖掘疲劳等级。
     */
    @Unique
    private static final int CONTROLLED_ELDER_GUARDIAN_EFFECT_AMPLIFIER = 2;
    /**
     * 效果刷新阈值。
     */
    @Unique
    private static final int CONTROLLED_ELDER_GUARDIAN_REFRESH_MARGIN = 40;

    public MixinElderGuardian(EntityType<? extends Guardian> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * 注入 {@code customServerAiStep} 返回点：为敌对玩家施加/刷新挖掘疲劳并播放远古守卫者提示事件。
     */
    @Inject(method = "customServerAiStep()V", at = @At("RETURN"))
    private void injectCustomServerAiStep(CallbackInfo ci) {
        if (!MobControlledData.isControlledEntity(this) || this.tickCount % 20 != 0) {
            return;
        }

        LivingEntity target = this.getTarget();
        if (target instanceof ServerPlayer serverPlayer) {
            MobEffectInstance effect = new MobEffectInstance(
                    MobEffects.DIG_SLOWDOWN,
                    CONTROLLED_ELDER_GUARDIAN_EFFECT_DURATION,
                    CONTROLLED_ELDER_GUARDIAN_EFFECT_AMPLIFIER
            );

            if (!serverPlayer.hasEffect(MobEffects.DIG_SLOWDOWN)
                    || Objects.requireNonNull(serverPlayer.getEffect(MobEffects.DIG_SLOWDOWN))
                    .getAmplifier() < CONTROLLED_ELDER_GUARDIAN_EFFECT_AMPLIFIER
                    || Objects.requireNonNull(serverPlayer.getEffect(MobEffects.DIG_SLOWDOWN))
                    .endsWithin(CONTROLLED_ELDER_GUARDIAN_REFRESH_MARGIN)) {
                serverPlayer.addEffect(new MobEffectInstance(effect), this);
                serverPlayer.connection.send(new ClientboundGameEventPacket(
                        ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT,
                        this.isSilent() ? 0 : 1
                ));
            }
        }
    }
}