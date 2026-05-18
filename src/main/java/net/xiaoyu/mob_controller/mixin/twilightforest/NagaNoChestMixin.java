package net.xiaoyu.mob_controller.mixin.twilightforest;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.config.FeatureConfig;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import twilightforest.entity.boss.IBossLootBuffer;
import twilightforest.entity.boss.Naga;

@Mixin(Naga.class)
public abstract class NagaNoChestMixin implements IBossLootBuffer {

    // 拦截 die 方法中的 saveDropsIntoBoss
    @Redirect(
            method = "die",
            at = @At(value = "INVOKE",
                    target = "Ltwilightforest/entity/boss/IBossLootBuffer;saveDropsIntoBoss(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/storage/loot/LootParams;Lnet/minecraft/server/level/ServerLevel;)V",
                    remap = false)
    )
    private <T extends LivingEntity & IBossLootBuffer> void redirectSaveDropsIntoBoss(
            T boss, LootParams params, ServerLevel level) {
        if (boss instanceof Naga naga
                && Config.ENABLE_RESPAWN.get()
                && FeatureConfig.PREVENT_DROPS_ON_RESPAWN.get()
                && MobControlledData.isControlledEntity(naga)) {
            return; // 跳过箱子生成
        }
        IBossLootBuffer.saveDropsIntoBoss(boss, params, level);
    }

    // 拦截 remove 方法中的 depositDropsIntoChest
    @Redirect(
            method = "remove",
            at = @At(value = "INVOKE",
                    target = "Ltwilightforest/entity/boss/IBossLootBuffer;depositDropsIntoChest(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/server/level/ServerLevel;)V",
                    remap = false)
    )
    private <T extends LivingEntity & IBossLootBuffer> void redirectDepositDropsIntoChest(
            T boss, BlockState chest, BlockPos pos, ServerLevel level) {
        if (boss instanceof Naga naga
                && Config.ENABLE_RESPAWN.get()
                && FeatureConfig.PREVENT_DROPS_ON_RESPAWN.get()
                && MobControlledData.isControlledEntity(naga)) {
            return;
        }
        IBossLootBuffer.depositDropsIntoChest(boss, chest, pos, level);
    }
}