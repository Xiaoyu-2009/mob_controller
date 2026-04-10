package net.xiaoyu.mob_controller.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.PiglinBruteSpecificSensor;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;
/**
 * 猪灵蛮兵专用感知器注入。
 *
 * <p>受控猪灵蛮兵感知阶段清空最近可见仇敌记忆，避免误锁定友方。</p>
 */

@Mixin(PiglinBruteSpecificSensor.class)
public abstract class MixinPiglinBruteSpecificSensor {
    /**
     * 包装 {@code Brain#setMemory}：受控猪灵蛮兵时清空最近可见仇敌记忆。
     */
    @WrapOperation(method = "doTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/Brain;setMemory(Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;Ljava/util/Optional;)V",
                    ordinal = 0
            )
    )
    private <U extends Mob> void wrapOperationDoTick(Brain<?> instance, MemoryModuleType<U> memoryType, Optional<? extends U> memory, Operation<Void> original, @Local(argsOnly = true) LivingEntity entity) {
        if (memoryType.equals(MemoryModuleType.NEAREST_VISIBLE_NEMESIS) && MobControlledData.isControlledEntity(entity)) {
            if (memory.isPresent()) {
                memory = Optional.empty();
            }
        }
        original.call(instance, memoryType, memory);
    }
}
