package net.xiaoyu.mob_controller.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.xiaoyu.mob_controller.entity.EntityControlledWitch;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;
import javax.annotation.Nullable;

/**
 * 最近可攻击目标 AI 注入。
 *
 * <p>在目标谓词中追加受控生物敌友过滤，避免锁定友方目标。</p>
 */
@Mixin(NearestAttackableTargetGoal.class)
public abstract class MixinNearestAttackableTargetGoal {
    /**
     * 包装 {@code TargetingConditions#selector}：拼接受控生物敌友过滤谓词。
     */
    @WrapOperation(
        method = "<init>(Lnet/minecraft/world/entity/Mob;Ljava/lang/Class;IZZLjava/util/function/Predicate;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;selector(Ljava/util/function/Predicate;)Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;"
        )
    )
    private TargetingConditions wrapSelector(
        TargetingConditions instance, @Nullable Predicate<LivingEntity> customPredicate,
        Operation<TargetingConditions> original, @Local(argsOnly = true) Mob mob
    ) {
        Predicate<LivingEntity> predicate = customPredicate != null ? customPredicate : livingEntity -> true;
        predicate = predicate.and(livingEntity -> !MobControlledData.isControlledEntity(mob)
                                                || (mob instanceof EntityControlledWitch)
                                                || MobControlUtil.canKeepCombatTarget(mob, livingEntity));
        return original.call(instance, predicate);
    }
}
