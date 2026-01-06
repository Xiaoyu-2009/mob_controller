package net.xiaoyu.mob_controller.mixin;

import net.xiaoyu.mob_controller.util.MobControlledData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(PiglinAi.class)
public class PiglinAiMixin {

    // 被控制的猪灵寻找目标行为
    @Inject(method = "findNearestValidAttackTarget", at = @At("HEAD"), cancellable = true)
    private static void excludeOwnerFromTargeting(Piglin piglin, CallbackInfoReturnable<Optional<? extends LivingEntity>> cir) {
        if (MobControlledData.isControlledMob(piglin)) {
            UUID ownerUUID = MobControlledData.getControllerUUID(piglin);
            Brain<Piglin> brain = piglin.getBrain();

            // 愤怒目标是否是主人
            Optional<LivingEntity> angerTarget = BehaviorUtils.getLivingEntityFromUUIDMemory(piglin, MemoryModuleType.ANGRY_AT);
            if (angerTarget.isPresent() && angerTarget.get().getUUID().equals(ownerUUID)) {
                brain.eraseMemory(MemoryModuleType.ANGRY_AT);
            }

            // 最近的可见攻击玩家是否是主人
            Optional<Player> nearestAttackablePlayer = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
            if (nearestAttackablePlayer.isPresent() && nearestAttackablePlayer.get().getUUID().equals(ownerUUID)) {
                brain.eraseMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
            }

            // 最近的可攻击目标是否是主人
            Optional<Player> nearestTargetablePlayer = brain.getMemory(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD);
            if (nearestTargetablePlayer.isPresent() && nearestTargetablePlayer.get().getUUID().equals(ownerUUID)) {
                brain.eraseMemory(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD);
            }
        }
    }
}