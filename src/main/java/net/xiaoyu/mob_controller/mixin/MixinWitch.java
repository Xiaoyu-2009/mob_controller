package net.xiaoyu.mob_controller.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xiaoyu.mob_controller.registry.ModEffects;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 为受控女巫定制投掷药水逻辑与友方支援行为。
 */
@Mixin(Witch.class)
public abstract class MixinWitch {

    // 支援投掷冷却（刻）
    @Unique
    private int supportCooldown = 0;

    // ========== 支援逻辑 (aiStep 尾部注入) ==========

    @Inject(method = "aiStep", at = @At("RETURN"))
    private void onAiStep(CallbackInfo ci) {
        Witch witch = (Witch) (Object) this;
        if (!MobControlledData.isControlledEntity(witch)) {
            return;
        }

        Level level = witch.level();
        if (level.isClientSide) {
            return;
        }

        if (supportCooldown > 0) {
            supportCooldown--;
        } else {
            AABB searchArea = witch.getBoundingBox().inflate(16.0D);
            Player controller = MobControlledData.getController(witch, level);
            List<LivingEntity> allies = level.getEntitiesOfClass(LivingEntity.class, searchArea,
                    e -> e.isAlive() && isAlly(witch, controller, e) && (needsHealing(e) || needsFireResistance(e)));

            if (!allies.isEmpty()) {
                allies.sort(Comparator.comparingDouble(e -> {
                    if (needsFireResistance(e)) return 0.0;
                    if (needsHealing(e)) return e.getHealth() / e.getMaxHealth();
                    return 1.0;
                }));
                LivingEntity target = allies.get(0);
                throwSupportPotion(witch, target);
                supportCooldown = 40; // 冷却 2 秒
            }
        }
    }

    // ========== 攻击投掷逻辑完全接管 ==========

    @Inject(method = "performRangedAttack", at = @At("HEAD"), cancellable = true)
    private void onPerformRangedAttack(LivingEntity target, float distanceFactor, CallbackInfo ci) {
        Witch witch = (Witch) (Object) this;
        if (!MobControlledData.isControlledEntity(witch)) {
            return;
        }
        // 受控女巫完全自定义投掷行为
        performCustomRangedAttack(witch, target);
        ci.cancel();
    }

    // ---------- 辅助方法 ----------

    /**
     * 判断目标是否为友方（控制者本人、同控制者生物、控制者的驯服宠物）。
     */
    @Unique
    private boolean isAlly(Witch witch, @Nullable Player controller, LivingEntity target) {
        // 本身就是自己
        if (target == witch) return true;
        // 原版盟友判定（利用模组注入的 isAlliedTo）
        if (witch.isAlliedTo(target)) return true;
        // 控制者本人
        if (controller != null && target == controller) return true;
        // 控制者的驯服宠物
        if (controller != null && target instanceof TamableAnimal tamable) {
            UUID ownerUUID = tamable.getOwnerUUID();
            if (ownerUUID != null && ownerUUID.equals(controller.getUUID())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否需要治疗（生命 ≤ max(4, 40% 最大生命)）。
     */
    @Unique
    private boolean needsHealing(LivingEntity e) {
        float health = e.getHealth();
        float maxHealth = e.getMaxHealth();
        return health <= Math.max(4.0F, maxHealth * 0.4F);
    }

    /**
     * 判断是否需要抗火（着火或受火焰伤害，且无抗火效果）。
     */
    @Unique
    private boolean needsFireResistance(LivingEntity e) {
        return (e.isOnFire() || (e.getLastDamageSource() != null && e.getLastDamageSource().is(DamageTypeTags.IS_FIRE)))
                && !e.hasEffect(MobEffects.FIRE_RESISTANCE);
    }

    /**
     * 向友方投掷支援药水（抗火 / 再生 / 特殊治疗）。
     */
    @Unique
    private void throwSupportPotion(Witch witch, LivingEntity target) {
        Potion potion;
        if (needsFireResistance(target)) {
            potion = Potions.LONG_FIRE_RESISTANCE;
        } else if (needsHealing(target)) {
            if (target.getHealth() <= 4.0F || target.hasEffect(MobEffects.REGENERATION)) {
                potion = ModEffects.SPECIAL_HEALING.get(); // 瞬间大治疗
            } else {
                potion = Potions.REGENERATION;
            }
        } else {
            return;
        }

        ThrownPotion thrownpotion = new ThrownPotion(witch.level(), witch);
        thrownpotion.setItem(PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), potion));
        double d0 = target.getX() - witch.getX();
        double d1 = target.getEyeY() - 1.1F - witch.getY();
        double d2 = target.getZ() - witch.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        thrownpotion.shoot(d0, d1 + d3 * 0.2D, d2, 0.75F, 8.0F);
        if (!witch.isSilent()) {
            witch.level().playSound(null, witch.getX(), witch.getY(), witch.getZ(),
                    SoundEvents.WITCH_THROW, witch.getSoundSource(), 1.0F, 0.8F + witch.getRandom().nextFloat() * 0.4F);
        }
        witch.level().addFreshEntity(thrownpotion);
    }

    /**
     * 对敌对目标投掷伤害类药水。
     *
     * @param potion 药水类型（HARMING / HEALING 等）
     */
    @Unique
    private void performDamagePotionAttack(Witch witch, LivingEntity target, Potion potion) {
        Vec3 vec3 = target.getDeltaMovement();
        double d0 = target.getX() + vec3.x - witch.getX();
        double d1 = target.getEyeY() - 1.1F - witch.getY();
        double d2 = target.getZ() + vec3.z - witch.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        ThrownPotion thrownpotion = new ThrownPotion(witch.level(), witch);
        thrownpotion.setItem(PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), potion));
        thrownpotion.setXRot(thrownpotion.getXRot() - -20.0F);
        thrownpotion.shoot(d0, d1 + d3 * 0.2D, d2, 0.75F, 8.0F);
        if (!witch.isSilent()) {
            witch.level().playSound(null, witch.getX(), witch.getY(), witch.getZ(),
                    SoundEvents.WITCH_THROW, witch.getSoundSource(), 1.0F, 0.8F + witch.getRandom().nextFloat() * 0.4F);
        }
        witch.level().addFreshEntity(thrownpotion);
    }

    /**
     * 完整的自定义投掷逻辑，完全替代原版 performRangedAttack。
     */
    @Unique
    private void performCustomRangedAttack(Witch witch, LivingEntity target) {
        // 1. 友方 ➜ 支援
        Player controller = MobControlledData.getController(witch, witch.level());
        if (isAlly(witch, controller, target)) {
            throwSupportPotion(witch, target);
            return;
        }

        // 2. 灾厄村民（Raider） ➜ 直接伤害药水
        if (target instanceof Raider) {
            performDamagePotionAttack(witch, target, Potions.HARMING);
            return;
        }

        // 3. 亡灵生物 ➜ 瞬间治疗药水（对亡灵造成伤害）
        if (target.isInvertedHealAndHarm()) {
            performDamagePotionAttack(witch, target, Potions.HEALING);
            return;
        }

        // 4. 其他敌对目标 ➜ 模仿原版药水选择逻辑（但排除友方、灾厄、亡灵）
        Vec3 vec3 = target.getDeltaMovement();
        double d0 = target.getX() + vec3.x - witch.getX();
        double d1 = target.getEyeY() - 1.1F - witch.getY();
        double d2 = target.getZ() + vec3.z - witch.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);

        Potion potion;
        if (d3 >= 8.0D && !target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            potion = Potions.SLOWNESS;
        } else if (target.getHealth() >= 8.0F && !target.hasEffect(MobEffects.POISON)) {
            potion = Potions.POISON;
        } else if (d3 <= 3.0D && !target.hasEffect(MobEffects.WEAKNESS) && witch.getRandom().nextFloat() < 0.25F) {
            potion = Potions.WEAKNESS;
        } else {
            potion = Potions.HARMING;
        }

        performDamagePotionAttack(witch, target, potion);
    }
}