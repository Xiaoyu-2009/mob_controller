package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.xiaoyu.mob_controller.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 混入 LivingEntity 的 hurt 方法，阻止玩家对自己骑乘的实体造成伤害。
 * 无论该坐骑是否受控，只要玩家当前正骑在它身上，就无法攻击它。
 */
@Mixin(LivingEntity.class)
public abstract class MixinPlayerAttackRidingMob {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        // 检查配置开关
        if (!Config.PREVENT_PLAYER_ATTACK_OWN_MOUNT.get()) {
            return;
        }

        // 获取攻击者（处理投射物，追溯到发射者）
        Entity attacker = source.getEntity();
        if (source.getDirectEntity() instanceof Projectile projectile) {
            Entity projectileOwner = projectile.getOwner();
            if (projectileOwner instanceof LivingEntity) {
                attacker = projectileOwner;
            }
        }

        // 攻击者必须是玩家
        if (!(attacker instanceof Player player)) {
            return;
        }

        // 被攻击者必须是当前玩家骑乘的实体
        LivingEntity target = (LivingEntity) (Object) this;
        if (player.getVehicle() == target) {
            // 取消伤害
            cir.setReturnValue(false);
        }
    }
}