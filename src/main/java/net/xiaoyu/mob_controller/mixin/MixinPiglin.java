package net.xiaoyu.mob_controller.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/**
 * 猪灵行为注入。
 *
 * <p>为受控猪灵补充友方火抗投掷逻辑，并限制仇恨与拾取行为。</p>
 */
@Mixin(Piglin.class)
public abstract class MixinPiglin extends AbstractPiglin implements CrossbowAttackMob, InventoryCarrier {
    public MixinPiglin(EntityType<? extends AbstractPiglin> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * 注入 {@code performRangedAttack} 头部：对友方投掷抗火药水并取消原始弩攻击。
     */
    @Inject(method = "performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V", at = @At("HEAD"), cancellable = true)
    private void injectPerformRangedAttack(LivingEntity target, float distanceFactor, CallbackInfo ci) {
        if (MobControlledData.isControlledEntity(this) && !MobControlUtil.isEnemy(this, target)) {
            this.setTarget(null);

            Vec3 vec3 = target.getDeltaMovement();
            double d0 = target.getX() + vec3.x - this.getX();
            double d1 = target.getEyeY() - (double) 1.1F - this.getY();
            double d2 = target.getZ() + vec3.z - this.getZ();
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);

            ThrownPotion thrownpotion = new ThrownPotion(this.level(), this);
            thrownpotion.setItem(PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.FIRE_RESISTANCE));
            thrownpotion.setXRot(thrownpotion.getXRot() + 20.0F);
            thrownpotion.shoot(d0, d1 + d3 * 0.2D, d2, 0.75F, 8.0F);
            if (!this.isSilent()) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITCH_THROW, this.getSoundSource(), 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
            }

            this.level().addFreshEntity(thrownpotion);

            ci.cancel();
        }
    }

    /**
     * 注入 {@code customServerAiStep} 头部：周期性为着火友方补投抗火药水。
     */
    @Inject(method = "customServerAiStep()V", at = @At("HEAD"))
    private void injectCustomServerAiStep(CallbackInfo ci) {
        if (MobControlledData.isControlledEntity(this) && this.getPersistentData().getLong("mob_controller.piglinFireResistancePotion") < this.tickCount) {
            double followRange = this.getAttributeValue(Attributes.FOLLOW_RANGE);
            LivingEntity target = this.level().getNearestEntity(this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(followRange, 4, followRange), t -> true),
                    TargetingConditions.forNonCombat().range(followRange)
                            .selector(livingEntity -> !MobControlUtil.isEnemy(this, livingEntity)),
                    this, this.getX(), this.getEyeY(), this.getZ());

            if (target != null) {
                boolean isOnFire = target.isOnFire() || target.getLastDamageSource() != null && target.getLastDamageSource().is(DamageTypeTags.IS_FIRE);
                if (isOnFire && !target.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                    this.performRangedAttack(target, 1.6F);
                    this.getPersistentData().putLong("mob_controller.piglinFireResistancePotion", this.tickCount + 20);
                }
            }
        }
    }

    /**
     * 包装 {@code wantsToPickUp}：受控猪灵仅允许拾取猪灵货币。
     */
    @WrapOperation(method = "wantsToPickUp(Lnet/minecraft/world/item/ItemStack;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/piglin/Piglin;canPickUpLoot()Z"))
    private boolean wrapOperationWantsToPickUp(Piglin instance, Operation<Boolean> original, @Local(argsOnly = true) ItemStack itemStack) {
        if (MobControlledData.isControlledEntity(instance)) {
            return itemStack.isPiglinCurrency();
        }
        return original.call(instance);
    }
}
