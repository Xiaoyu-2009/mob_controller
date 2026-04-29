package net.xiaoyu.mob_controller.event;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.xiaoyu.mob_controller.MobController;
import net.xiaoyu.mob_controller.capability.WaxedCapabilityProvider;

@Mod.EventBusSubscriber(modid = MobController.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WaxedHandler {

    // 突变怪物模组的类名（用于运行时检测）
    private static final String MUTANT_ENDERMAN_CLASS = "fuzs.mutantmonsters.world.entity.mutant.MutantEnderman";
    private static final String MUTANT_SNOW_GOLEM_CLASS = "fuzs.mutantmonsters.world.entity.mutant.MutantSnowGolem";

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (!(event.getTarget() instanceof LivingEntity target)) return;

        Level level = target.level();
        var hand = event.getHand();
        var stack = player.getItemInHand(hand);

        // 判断是否为特殊突变生物（无论 isSensitiveToWater 返回值如何）
        boolean isSpecialMutant = isMutantEnderman(target) || isMutantSnowGolem(target);

        // 判断是否为幻翼
        boolean isPhantom = target instanceof Phantom;

        // 如果不是特殊突变生物，且目标原本不怕水，则跳过
        if (!isSpecialMutant && !isPhantom && !target.isSensitiveToWater()) {
            return;
        }

        // ----- 蜜脾打蜡（仅当未打蜡时）-----
        if (stack.is(Items.HONEYCOMB)) {
            if (level.isClientSide) {
                player.swing(hand);
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }

            target.getCapability(WaxedCapabilityProvider.WAXED_CAPABILITY).ifPresent(cap -> {
                if (!cap.isWaxed()) {
                    cap.setWaxed(true);
                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                    level.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.HONEYCOMB_WAX_ON, SoundSource.PLAYERS, 1.0F, 1.0F);
                    spawnParticles(target, ParticleTypes.WAX_ON, 30);
                }
            });
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
        // ----- 斧头刮蜡（仅当已打蜡时）-----
        else if (stack.is(Items.WOODEN_AXE) || stack.is(Items.STONE_AXE) || stack.is(Items.IRON_AXE) ||
                stack.is(Items.GOLDEN_AXE) || stack.is(Items.DIAMOND_AXE) || stack.is(Items.NETHERITE_AXE)) {
            if (level.isClientSide) {
                player.swing(hand);
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }

            target.getCapability(WaxedCapabilityProvider.WAXED_CAPABILITY).ifPresent(cap -> {
                if (cap.isWaxed()) {
                    cap.setWaxed(false);
                    stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                    level.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.AXE_SCRAPE, SoundSource.PLAYERS, 1.0F, 1.0F);
                    spawnParticles(target, ParticleTypes.SCRAPE, 20);
                }
            });
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    // 检测是否为突变末影人（通过类名字符串，无硬依赖）
    private static boolean isMutantEnderman(LivingEntity entity) {
        return ModList.get().isLoaded("mutantmonsters") &&
                entity.getClass().getName().equals(MUTANT_ENDERMAN_CLASS);
    }

    // 检测是否为突变雪傀儡
    private static boolean isMutantSnowGolem(LivingEntity entity) {
        return ModList.get().isLoaded("mutantmonsters") &&
                entity.getClass().getName().equals(MUTANT_SNOW_GOLEM_CLASS);
    }

    // 粒子生成方法（与之前相同）
    private static void spawnParticles(LivingEntity entity, net.minecraft.core.particles.SimpleParticleType particleType, int count) {
        Level level = entity.level();
        AABB box = entity.getBoundingBox();
        double x = (box.minX + box.maxX) / 2.0;
        double y = (box.minY + box.maxY) / 2.0;
        double z = (box.minZ + box.maxZ) / 2.0;
        double dx = (box.maxX - box.minX) / 2.0;
        double dy = (box.maxY - box.minY) / 2.0;
        double dz = (box.maxZ - box.minZ) / 2.0;

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particleType, x, y, z, count, dx, dy, dz, 0.1);
        } else {
            for (int i = 0; i < count; i++) {
                double offX = (level.random.nextDouble() - 0.5) * (box.maxX - box.minX);
                double offY = (level.random.nextDouble() - 0.5) * (box.maxY - box.minY);
                double offZ = (level.random.nextDouble() - 0.5) * (box.maxZ - box.minZ);
                level.addParticle(particleType, x + offX, y + offY, z + offZ, 0, 0, 0);
            }
        }
    }
}