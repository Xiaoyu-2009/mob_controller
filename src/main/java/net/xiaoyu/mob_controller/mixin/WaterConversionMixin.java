package net.xiaoyu.mob_controller.mixin;

import net.xiaoyu.mob_controller.util.MobControlledData;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Zombie.class, Husk.class})
public class WaterConversionMixin {
    
    @Inject(method = "convertsInWater", at = @At("HEAD"), cancellable = true)
    private void preventWaterConversion(CallbackInfoReturnable<Boolean> cir) {
        Zombie zombie = (Zombie) (Object) this;

        // 被控制的僵尸/尸壳取消水中转换
        if (MobControlledData.isControlledEntity(zombie)) {
            cir.setReturnValue(false);
        }
    }
}