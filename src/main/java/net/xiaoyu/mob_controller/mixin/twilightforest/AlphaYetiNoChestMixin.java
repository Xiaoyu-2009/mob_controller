package net.xiaoyu.mob_controller.mixin.twilightforest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.config.FeatureConfig;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import twilightforest.entity.boss.AlphaYeti;

@Mixin(AlphaYeti.class)
public abstract class AlphaYetiNoChestMixin {

    @Redirect(
            method = "die",
            at = @At(value = "INVOKE",
                    target = "Ltwilightforest/loot/TFLootTables;entityDropsIntoContainer(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
                    remap = false)
    )
    private void redirectEntityDropsIntoContainer(LivingEntity entity, DamageSource source, BlockState chest, BlockPos pos) {
        if (entity instanceof AlphaYeti yeti
                && Config.ENABLE_RESPAWN.get()
                && FeatureConfig.PREVENT_DROPS_ON_RESPAWN.get()
                && MobControlledData.isControlledEntity(yeti)) {
            return; // 跳过箱子生成
        }
        twilightforest.loot.TFLootTables.entityDropsIntoContainer(entity, source, chest, pos);
    }
}