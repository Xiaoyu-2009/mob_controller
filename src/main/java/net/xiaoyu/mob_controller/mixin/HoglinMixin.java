package net.xiaoyu.mob_controller.mixin;

import net.xiaoyu.mob_controller.util.MobControlledData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(Hoglin.class)
public class HoglinMixin {

    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void onCustomServerAiStep(CallbackInfo ci) {
        Hoglin hoglin = (Hoglin) (Object) this;
        
        if (MobControlledData.isControlledMob(hoglin)) {
            Brain<Hoglin> brain = hoglin.getBrain();
            
            if (brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
                Optional<LivingEntity> attackTarget = brain.getMemory(MemoryModuleType.ATTACK_TARGET);
                
                if (attackTarget.isPresent()) {
                    LivingEntity target = attackTarget.get();

                    if (target instanceof Player) {
                        UUID controllerUUID = MobControlledData.getControllerUUID(hoglin);

                        if (controllerUUID != null && target.getUUID().equals(controllerUUID)) {
                            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
                        }
                    }
                }
            }
        }
    }
}