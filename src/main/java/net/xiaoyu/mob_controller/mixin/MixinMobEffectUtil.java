package net.xiaoyu.mob_controller.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;
/**
 * 状态效果分发工具注入。
 *
 * <p>过滤受控生物触发的范围效果，避免对友方或不应受影响的玩家生效。</p>
 */
@Mixin(MobEffectUtil.class)
public abstract class MixinMobEffectUtil {
    /**
     * 包装 {@code addEffectToPlayersAround} 内玩家筛选：为受控监守者/远古守卫者与友方过滤效果目标。
     */
    @WrapOperation(method = "addEffectToPlayersAround(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;DLnet/minecraft/world/effect/MobEffectInstance;I)Ljava/util/List;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;getPlayers(Ljava/util/function/Predicate;)Ljava/util/List;"
            )
    )
    private static List<ServerPlayer> wrapAddEffectToPlayersAround(ServerLevel instance, Predicate<? super ServerPlayer> predicate, Operation<List<ServerPlayer>> original,
                                                                   @Local(argsOnly = true) @Nullable Entity source,
                                                                   @Local(argsOnly = true) MobEffectInstance effect) {
        if (!(source instanceof Mob mob)) {
            return original.call(instance, predicate);
        }

        if (mob instanceof Warden && MobControlledData.isControlledEntity(mob) && effect.getEffect() == MobEffects.DARKNESS) {
            return List.of();
        }

        if (mob instanceof ElderGuardian && MobControlledData.isControlledEntity(mob) && effect.getEffect() == MobEffects.DIG_SLOWDOWN) {
            return List.of();
        }

        predicate = predicate.and(player -> !MobControlledData.isControlledEntity(mob) || !MobControlUtil.isEnemy(mob, (Entity) player));
        return original.call(instance, predicate);
    }
}
