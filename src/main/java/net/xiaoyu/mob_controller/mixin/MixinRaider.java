package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Mixin(Raider.class)
public abstract class MixinRaider extends PatrollingMonster {
    protected MixinRaider(EntityType<? extends PatrollingMonster> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * 每刻检查：若为受控状态，立即移除主动攻击类目标选择器。
     */
    @Inject(method = "aiStep()V", at = @At("HEAD"))
    private void onAiStep(CallbackInfo ci) {
        Raider raider = (Raider) (Object) this;
        if (MobControlledData.isControlledEntity(raider)) {
            new ArrayList<>(raider.targetSelector.getAvailableGoals()).forEach(wrapped -> {
                Goal g = wrapped.getGoal();
                if (g instanceof HurtByTargetGoal || g instanceof NearestAttackableTargetGoal) {
                    raider.targetSelector.removeGoal(g);
                }
            });
        }
    }

    /**
     * 受控状态下不参与袭击。
     */
    @Inject(method = "getCurrentRaid()Lnet/minecraft/world/entity/raid/Raid;", at = @At("RETURN"), cancellable = true)
    private void injectGetCurrentRaid(CallbackInfoReturnable<Raid> cir) {
        if (MobControlledData.isControlledEntity(this)) {
            cir.setReturnValue(null);
        }
    }
}