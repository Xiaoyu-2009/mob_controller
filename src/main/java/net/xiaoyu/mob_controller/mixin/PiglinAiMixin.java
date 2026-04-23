package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * 猪灵 AI 行为注入。
 *
 * <p>受控猪灵会过滤玩家相关仇恨记忆，避免自动敌对玩家。</p>
 */
@Mixin(PiglinAi.class)
public class PiglinAiMixin {
    @Inject(method = "findNearestValidAttackTarget", at = @At("HEAD"))
    private static void clearPlayerHostilityMemory(Piglin piglin, CallbackInfoReturnable<Optional<? extends LivingEntity>> cir) {
        if (!MobControlledData.isControlledEntity(piglin)) {
            return;
        }

        Brain<Piglin> brain = piglin.getBrain();
        Optional<LivingEntity> angerTarget = BehaviorUtils.getLivingEntityFromUUIDMemory(piglin, MemoryModuleType.ANGRY_AT);
        if (angerTarget.isPresent() && !MobControlUtil.canKeepCombatTarget(piglin, angerTarget.get())) {
            brain.eraseMemory(MemoryModuleType.ANGRY_AT);
        }

        Optional<? extends LivingEntity> attackTarget = brain.getMemory(MemoryModuleType.ATTACK_TARGET);
        if (attackTarget.isPresent() && !MobControlUtil.canKeepCombatTarget(piglin, attackTarget.get())) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }

        Optional<? extends LivingEntity> nearestVisibleNemesis = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS);
        if (nearestVisibleNemesis.isPresent() && !MobControlUtil.canKeepCombatTarget(piglin, nearestVisibleNemesis.get())) {
            brain.eraseMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS);
        }

        // 仅清理不允许继续作为战斗目标的玩家记忆。
        Optional<Player> attackablePlayer = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
        if (attackablePlayer.isPresent() && !MobControlUtil.canKeepCombatTarget(piglin, attackablePlayer.get())) {
            brain.eraseMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
        }
    }

    @Inject(method = "findNearestValidAttackTarget", at = @At("RETURN"), cancellable = true)
    private static void filterNeutralPlayerTarget(Piglin piglin, CallbackInfoReturnable<Optional<? extends LivingEntity>> cir) {
        if (!MobControlledData.isControlledEntity(piglin)) {
            return;
        }

        Optional<? extends LivingEntity> result = cir.getReturnValue();
        if (result.isPresent()) {
            LivingEntity target = result.get();
            if (!MobControlUtil.canKeepCombatTarget(piglin, target)) {
                cir.setReturnValue(Optional.empty());
            }
        }
    }
}

