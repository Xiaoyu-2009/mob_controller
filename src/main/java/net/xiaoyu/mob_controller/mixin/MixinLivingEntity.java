package net.xiaoyu.mob_controller.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**
 * 混入 LivingEntity，实现骑乘控制的核心逻辑：
 * - 陆地生物：原版骑乘控制（跳跃由空格键触发）
 * - 水生生物：守卫者/海豚风格的水中控制，可配置离水下马
 * - 飞行生物：三维自由飞行，移动方向完全由玩家视角（俯仰角+偏航角）决定
 * - 飞行陆地生物：地面行走时使用陆地控制，按住空格上升（类似飞行）
 * - 两栖生物：在水中使用水生控制，在陆地上使用陆地控制（可配置跳跃高度）
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {
    @Shadow
    public float yBodyRot;
    @Shadow
    public float yHeadRot;

    public MixinLivingEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * 注入 canAttack：对受控生物追加敌友判定限制。
     */
    @SuppressWarnings("ConstantValue")
    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("RETURN"), cancellable = true)
    private void injectCanAttack(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            if ((Object) this instanceof LivingEntity mob) {
                if (MobControlledData.isControlledEntity(mob)
                        && !MobControlUtil.canKeepCombatTarget(mob, target)) {
                    cir.cancel();
                }
            }
        }
    }

    /**
     * 注入 hurt：拦截友伤并触发非受控生物反击受控生物。
     */
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        Entity attacker = source.getEntity();

        // 处理投射物：追溯到发射者
        if (source.getDirectEntity() instanceof Projectile projectile) {
            Entity projectileOwner = projectile.getOwner();
            if (projectileOwner instanceof LivingEntity) {
                attacker = projectileOwner;
            }
        }

        // 拦截受控生物对友方的所有伤害
        if (attacker instanceof LivingEntity mob && MobControlledData.isControlledEntity(mob)) {
            if (MobControlUtil.isAlly(mob, livingEntity)) {
                cir.cancel();
                return;
            }
        }

        // 其他生物受到被控制生物攻击的反击
        if (attacker instanceof LivingEntity controlledMob && MobControlledData.isControlledEntity(controlledMob)
                && livingEntity instanceof Mob otherMob && !MobControlledData.isControlledEntity(otherMob)) {
            if (source.getEntity() instanceof Player player) {
                if (player.isCreative() || player.isSpectator()) {
                    return;
                }
            }
            MobControlledData.markSystemAttack(otherMob);
            otherMob.setTarget(controlledMob);
        }
    }

    /**
     * 注入 tickRidden：同步可骑乘生物的朝向，为水生生物处理离水下马，
     * 并为陆地/两栖生物（陆地形态）处理跳跃。
     */
    @Inject(method = "tickRidden(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void injectTickRidden(Player player, Vec3 travelVector, CallbackInfo ci) {
        Object thiz = this;
        if (thiz instanceof Mob mob && MobControlUtil.isDirectRideableControlledMob(mob)) {
            MobControlUtil.RideableType type = MobControlUtil.getRideableType(mob);

            // 水生生物离水强制下马（根据配置）
            if (type == MobControlUtil.RideableType.AQUATIC && MobControlUtil.shouldDismountOnLeaveWater(mob)) {
                if (!mob.isInWaterOrBubble() && mob.getControllingPassenger() == player) {
                    player.stopRiding();
                    return;
                }
            }

            // 同步旋转角度
            this.setRot(player.getYRot(), 0.0F);// this.setRot(player.getYRot(), player.getXRot() * 0.5F);
            this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();

            // 处理陆地跳跃（LAND 以及 AMPHIBIAN 的陆地形态）
            boolean isLandBased = (type == MobControlUtil.RideableType.LAND) ||
                    (type == MobControlUtil.RideableType.AMPHIBIAN && !mob.isInWaterOrBubble()) ||
                    (type == MobControlUtil.RideableType.FLYING_LAND && mob.onGround());
            if (isLandBased && mob.getControllingPassenger() == player) {
                AccessorLivingEntity accessor = (AccessorLivingEntity) player;
                boolean isJumping = accessor.mob_controller$getJumping();
                if (isJumping && mob.onGround()) {
                    double jumpHeight;
                    if (type == MobControlUtil.RideableType.LAND) {
                        jumpHeight = MobControlUtil.getLandJumpHeight(mob);
                    } else {
                        jumpHeight = MobControlUtil.getAmphibianLandJumpHeight(mob);
                    }
                    mob.setDeltaMovement(mob.getDeltaMovement().x, jumpHeight, mob.getDeltaMovement().z);
                    mob.hasImpulse = true;
                    ForgeHooks.onLivingJump(mob); // 兼容其他模组
                }
            }
        }
    }

    private static boolean hasCustomRideSpeed(Mob mob) {
        ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (key == null) return false;
        String id = key.toString();
        for (String entry : Config.RIDE_CUSTOM_SPEED.get()) {
            if (entry.startsWith(id + ",")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 注入 getRiddenInput：为不同骑乘类型提供基于玩家输入的移动向量。
     */
    @Inject(
            method = "getRiddenInput(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void injectGetRiddenInput(Player player, Vec3 travelVector, CallbackInfoReturnable<Vec3> cir) {
        Object thiz = this;
        if (thiz instanceof Mob livingMob) {
            MobControlUtil.RideableType type = MobControlUtil.getRideableType(livingMob);
            if (type == null) return;
            boolean hasCustomSpeed = hasCustomRideSpeed(livingMob);

            double strafe = player.xxa;          // 左右（A/D）
            double forward = player.zza;         // 前后（W/S）
            if (forward <= 0.0) forward *= 0.25; // 后退减速

            AccessorLivingEntity accessor = (AccessorLivingEntity) player;
            boolean isJumping = accessor.mob_controller$getJumping();

            switch (type) {
                case LAND:
                    // 陆地：基础移动向量
                    Vec3 waterVec = new Vec3(strafe * 0.5, 0, forward);
                    // 在水中时，允许空格上浮（与水生行为类似）
                    if (livingMob.isInWaterOrBubble() && isJumping) {
                        waterVec = new Vec3(strafe * 0.5, 1, forward);
                    }
                    cir.setReturnValue(waterVec);
                    return;

                case AQUATIC:
                    // 水生：类似守卫者/海豚的输入（基于玩家俯仰角）
                    if (hasCustomSpeed) {
                        return;
                    }
                    double x = strafe * 0.5;
                    double y = 0;
                    double z = forward;
                    if (z != 0) {
                        double i = Math.cos(player.getXRot() * Math.PI / 180.0);
                        double j = -Math.sin(player.getXRot() * Math.PI / 180.0);
                        if (z < 0.0) {
                            i *= -0.5;
                            j *= -0.5;
                        }
                        y = j;
                        z = i;
                    }
                    if (isJumping && this.isInWaterOrBubble()) {
                        y += 0.5;
                    }
                    cir.setReturnValue(new Vec3(x, y, z));
                    return;

                case FLYING:
                    // 纯飞行生物：完全自由三维飞行
                    if (hasCustomSpeed) {
                        return;
                    }
                    double flyingX = strafe * 0.5;
                    double flyingY = 0;
                    double flyingZ = forward;
                    if (flyingZ != 0) {
                        double i = Math.cos(player.getXRot() * Math.PI / 180.0);
                        double j = -Math.sin(player.getXRot() * Math.PI / 180.0);
                        if (flyingZ < 0.0) {
                            i *= -0.5;
                            j *= -0.5;
                        }
                        flyingY = j;
                        flyingZ = i;
                    }
                    if (isJumping) {
                        flyingY += 0.5;
                    }
                    cir.setReturnValue(new Vec3(flyingX, flyingY, flyingZ));
                    return;

                case FLYING_LAND:
                    // 飞行陆地：地面时使用陆地控制，空中/下落时使用飞行控制
                    if (livingMob.onGround()) {
                        // 地面模式：纯平面移动，跳跃由 tickRidden 处理
                        cir.setReturnValue(new Vec3(strafe * 0.5, 0, forward));
                    } else {
                        if (hasCustomSpeed) {
                            return;
                        }
                        // 飞行模式：三维自由移动（同 FLYING）
                        double flyX = strafe * 0.5;
                        double flyY = 0;
                        double flyZ = forward;
                        if (flyZ != 0) {
                            double i = Math.cos(player.getXRot() * Math.PI / 180.0);
                            double j = -Math.sin(player.getXRot() * Math.PI / 180.0);
                            if (flyZ < 0.0) {
                                i *= -0.5;
                                j *= -0.5;
                            }
                            flyY = j;
                            flyZ = i;
                        }
                        if (isJumping) {
                            flyY += 0.5;
                        }
                        cir.setReturnValue(new Vec3(flyX, flyY, flyZ));
                    }
                    return;

                case AMPHIBIAN:
                    // 两栖：根据是否在水中选择控制方式
                    if (livingMob.isInWaterOrBubble()) {
                        if (hasCustomSpeed) {
                            return;
                        }
                        // 水中：使用水生控制逻辑
                        double aqX = strafe * 0.5;
                        double aqY = 0;
                        double aqZ = forward;
                        if (aqZ != 0) {
                            double i = Math.cos(player.getXRot() * Math.PI / 180.0);
                            double j = -Math.sin(player.getXRot() * Math.PI / 180.0);
                            if (aqZ < 0.0) {
                                i *= -0.5;
                                j *= -0.5;
                            }
                            aqY = j;
                            aqZ = i;
                        }
                        if (isJumping) {
                            aqY += 0.5;
                        }
                        if (!livingMob.isInWaterOrBubble()) {
                            aqX = 0;
                            aqZ = 0;
                        }
                        cir.setReturnValue(new Vec3(aqX, aqY, aqZ));
                    } else {
                        // 陆地：使用陆地控制（移动向量，跳跃由 tickRidden 处理）
                        cir.setReturnValue(new Vec3(strafe * 0.5, 0, forward));
                    }
                    return;
            }
        }
    }

    /**
     * 注入 getRiddenSpeed：覆盖不同骑乘类型的移动速度。
     */
    @Inject(method = "getRiddenSpeed(Lnet/minecraft/world/entity/player/Player;)F", at = @At("HEAD"), cancellable = true)
    private void injectGetRiddenSpeed(Player player, CallbackInfoReturnable<Float> cir) {
        Object thiz = this;
        if (thiz instanceof LivingEntity mob && mob instanceof Mob livingMob) {
            MobControlUtil.RideableType type = MobControlUtil.getRideableType(livingMob);
            if (type == null) return;

            float speed = (float) mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
            switch (type) {
                case LAND:
                    cir.setReturnValue(speed);
                    break;
                case AQUATIC:
                    // 海豚速度降为 0.1 倍，其余水生原速
                    if (mob instanceof Dolphin) {
                        cir.setReturnValue(speed * 0.1f);
                    } else {
                        cir.setReturnValue(speed);
                    }
                    break;
                case FLYING:
                case FLYING_LAND:
                case AMPHIBIAN:
                    cir.setReturnValue(speed);
                    break;
            }
        }
    }
}