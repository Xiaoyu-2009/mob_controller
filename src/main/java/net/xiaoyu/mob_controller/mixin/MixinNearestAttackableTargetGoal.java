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

import javax.annotation.Nullable;
import java.util.function.Predicate;

@Mixin(NearestAttackableTargetGoal.class)
public abstract class MixinNearestAttackableTargetGoal {
    @WrapOperation(method = "<init>(Lnet/minecraft/world/entity/Mob;Ljava/lang/Class;IZZLjava/util/function/Predicate;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;selector(Ljava/util/function/Predicate;)Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;"
            )
    )
    private TargetingConditions wrapSelector(TargetingConditions instance, @Nullable Predicate<LivingEntity> customPredicate,
                                             Operation<TargetingConditions> original, @Local(argsOnly = true) Mob mob) {
        Predicate<LivingEntity> predicate = customPredicate != null ? customPredicate : livingEntity -> true;
        predicate = predicate.and(livingEntity -> !MobControlledData.isControlledEntity(mob) || !MobControlUtil.isEnemy(mob, livingEntity) || (mob instanceof EntityControlledWitch));
        return original.call(instance, predicate);
    }
}