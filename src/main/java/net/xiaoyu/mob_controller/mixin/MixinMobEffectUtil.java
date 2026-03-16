package net.xiaoyu.mob_controller.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

@Mixin(MobEffectUtil.class)
public abstract class MixinMobEffectUtil {
    @WrapOperation(method = "addEffectToPlayersAround(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;DLnet/minecraft/world/effect/MobEffectInstance;I)Ljava/util/List;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;getPlayers(Ljava/util/function/Predicate;)Ljava/util/List;"
            )
    )
    private static List<ServerPlayer> wrapAddEffectToPlayersAround(ServerLevel instance, Predicate<? super ServerPlayer> predicate, Operation<List<ServerPlayer>> original, @Local(argsOnly = true) @Nullable Entity source) {
        predicate = predicate.and(player -> source instanceof Mob mob
                && (!MobControlledData.isControlledEntity(mob) || !MobControlUtil.isEnemy(mob, (Entity) player)));
        return original.call(instance, predicate);
    }
}
