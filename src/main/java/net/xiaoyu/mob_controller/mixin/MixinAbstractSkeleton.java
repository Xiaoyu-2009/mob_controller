package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractSkeleton.class)
public abstract class MixinAbstractSkeleton {

    /**
     * 拦截 AbstractSkeleton.aiStep() 中的头盔耐久变更。
     * 受控骷髅（含流浪者）跳过 setDamageValue 调用，防止头盔损耗。
     */
    @Redirect(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;setDamageValue(I)V"
            )
    )
    private void preventHelmetDamageForControlledSkeleton(ItemStack helmet, int newDamage) {
        AbstractSkeleton skeleton = (AbstractSkeleton) (Object) this;
        if (MobControlledData.isControlledEntity(skeleton)) {
            return;
        }
        helmet.setDamageValue(newDamage);
    }

    /**
     * 修改骷髅射箭时的散射值（inaccuracy）。
     * 如果是受控状态，将散射值改为 0.0F（完美命中），否则保留原值。
     *
     * @param originalInaccuracy 原版计算出的散射值
     * @param skeleton           当前骷髅对象
     * @return 修改后的散射值
     */
    @ModifyVariable(
            method = "performRangedAttack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;shoot(DDDFF)V",
                    shift = At.Shift.BY,
                    by = 2
            ),
            ordinal = 0,
            argsOnly = true
    )
    private float enhanceAccuracyForControlledSkeleton(float originalInaccuracy, LivingEntity target, float distanceFactor) {
        AbstractSkeleton skeleton = (AbstractSkeleton) (Object) this;
        if (MobControlledData.isControlledEntity(skeleton)) {
            // 百步穿杨：散射为 0，箭矢绝对直线
            return 0.0F;
        }
        return originalInaccuracy;
    }
}