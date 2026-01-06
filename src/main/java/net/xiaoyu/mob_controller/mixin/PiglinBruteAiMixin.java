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

@Mixin(targets = "net.minecraft.world.entity.monster.piglin.PiglinBruteAi")
public class PiglinBruteAiMixin {

    // 被控制的猪灵蛮兵寻找目标行为
    @Inject(method = "findNearestValidAttackTarget", at = @At("HEAD"), cancellable = true)
    private static void excludeOwnerFromTargeting(AbstractPiglin piglin, CallbackInfoReturnable<Optional<? extends LivingEntity>> cir) {
        PiglinBrute brute = (PiglinBrute) piglin;
        if (MobControlledData.isControlledMob(brute)) {
            UUID ownerUUID = MobControlledData.getControllerUUID(brute);
            Brain<PiglinBrute> brain = brute.getBrain();

            // 愤怒目标是否是主人
            Optional<LivingEntity> angerTarget = BehaviorUtils.getLivingEntityFromUUIDMemory(brute, MemoryModuleType.ANGRY_AT);
            if (angerTarget.isPresent() && angerTarget.get().getUUID().equals(ownerUUID)) {
                brain.eraseMemory(MemoryModuleType.ANGRY_AT);
            }

            // 最近的可见攻击玩家是否是主人
            Optional<Player> nearestAttackablePlayer = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
            if (nearestAttackablePlayer.isPresent() && nearestAttackablePlayer.get().getUUID().equals(ownerUUID)) {
                brain.eraseMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
            }

            // 最近的可见仇恨目标是否是主人
            Optional<? extends LivingEntity> nearestVisibleNemesis = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS);
            if (nearestVisibleNemesis.isPresent() && nearestVisibleNemesis.get().getUUID().equals(ownerUUID)) {
                brain.eraseMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS);
            }
        }
    }
}