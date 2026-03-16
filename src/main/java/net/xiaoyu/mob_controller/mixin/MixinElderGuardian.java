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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;

@Mixin(ElderGuardian.class)
public abstract class MixinElderGuardian extends Guardian {
    public MixinElderGuardian(EntityType<? extends Guardian> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "customServerAiStep()V", at = @At("HEAD"))
    private void injectCustomServerAiStep(CallbackInfo ci) {
        if ((this.tickCount + this.getId()) % 1200 == 0 && MobControlledData.isControlledEntity(this)) {
            MobEffectInstance mobeffectinstance = new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 6000, 2);

            List<LivingEntity> list = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(50), (entity) ->
                    !this.isAlliedTo(entity) && MobControlUtil.isEnemy(this, entity)
                            && this.position().closerThan(entity.position(), 50)
                            && (!entity.hasEffect(MobEffects.DIG_SLOWDOWN)
                            || Objects.requireNonNull(entity.getEffect(MobEffects.DIG_SLOWDOWN)).getAmplifier() < mobeffectinstance.getAmplifier()
                            || Objects.requireNonNull(entity.getEffect(MobEffects.DIG_SLOWDOWN)).endsWithin(mobeffectinstance.getDuration() - 1)));
            list.forEach(entity -> {
                entity.addEffect(new MobEffectInstance(mobeffectinstance), this);
                if (entity instanceof ServerPlayer serverPlayer) {
                    serverPlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT, this.isSilent() ? 0 : 1));
                }
            });
        }
    }
}
