package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Zombie.class)
public abstract class MixinZombie {

    /**
     * 拦截 Zombie.aiStep() 中的头盔耐久变更。
     * 受控僵尸跳过 setDamageValue 调用，防止头盔损耗。
     */
    @Redirect(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;setDamageValue(I)V"
            )
    )
    private void preventHelmetDamageForControlledZombie(ItemStack helmet, int newDamage) {
        Zombie zombie = (Zombie) (Object) this;
        if (MobControlledData.isControlledEntity(zombie)) {
            // 受控：不应用耐久变化，直接返回
            return;
        }
        // 非受控：正常执行原版逻辑
        helmet.setDamageValue(newDamage);
    }
}