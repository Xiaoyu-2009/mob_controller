package net.xiaoyu.mob_controller.event;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraftforge.network.PacketDistributor;
import net.xiaoyu.mob_controller.MobController;
import net.xiaoyu.mob_controller.advancement.MobControllerTriggers;
import net.xiaoyu.mob_controller.capability.WaxedCapability;
import net.xiaoyu.mob_controller.capability.WaxedCapabilityProvider;
import net.xiaoyu.mob_controller.network.NetWorkManager;
import net.xiaoyu.mob_controller.network.SyncWaxedPacket;
import net.xiaoyu.mob_controller.network.client.ClientPacketHandler;

@Mod.EventBusSubscriber(modid = MobController.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WaxedHandler {

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
        boolean isPhantom = target instanceof Phantom;

        // 如果不是特殊突变生物，且目标原本不怕水，则跳过
        if (!isSpecialMutant && !isPhantom && !target.isSensitiveToWater()) {
            return;
        }

        // ----- 蜜脾处理 -----
        if (stack.is(Items.HONEYCOMB)) {
            // 客户端：根据本地缓存的打蜡状态决定是否干预
            if (level.isClientSide) {
                boolean waxed = ClientPacketHandler.isEntityWaxed(target.getId());
                if (waxed) {
                    // 已打蜡：完全放行，不挥臂，不取消事件
                    return;
                } else {
                    // 未打蜡：模组接管，播放手臂动画并取消原版事件
                    player.swing(hand);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
                return;
            }

            // 服务端：获取打蜡状态并决定是否执行打蜡
            target.getCapability(WaxedCapabilityProvider.WAXED_CAPABILITY).ifPresent(cap -> {
                if (!cap.isWaxed()) {
                    // 执行打蜡
                    cap.setWaxed(true);
                    // 同步给所有追踪该实体的玩家
                    broadcastWaxedState(target, true);

                    if (player instanceof ServerPlayer serverPlayer) {
                        MobControllerTriggers.WAX_WATER_SENSITIVE.trigger(serverPlayer, target);
                        MobController.grantRootAdvancementIfNeeded(serverPlayer);
                    }
                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                    level.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.HONEYCOMB_WAX_ON, SoundSource.PLAYERS, 1.0F, 1.0F);
                    spawnParticles(target, ParticleTypes.WAX_ON, 30);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
                // 已打蜡：服务端不做任何动作，也不取消事件，让原版处理
            });
            return;
        }
        // ----- 斧头处理 -----
        else if (stack.is(Items.WOODEN_AXE) || stack.is(Items.STONE_AXE) || stack.is(Items.IRON_AXE) ||
                stack.is(Items.GOLDEN_AXE) || stack.is(Items.DIAMOND_AXE) || stack.is(Items.NETHERITE_AXE)) {
            // 客户端：根据本地缓存决定是否干预
            if (level.isClientSide) {
                boolean waxed = ClientPacketHandler.isEntityWaxed(target.getId());
                if (!waxed) {
                    // 未打蜡：完全放行
                    return;
                } else {
                    // 已打蜡：模组接管，播放手臂动画并取消原版事件
                    player.swing(hand);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
                return;
            }

            // 服务端：获取打蜡状态
            target.getCapability(WaxedCapabilityProvider.WAXED_CAPABILITY).ifPresent(cap -> {
                if (cap.isWaxed()) {
                    // 执行刮蜡
                    cap.setWaxed(false);
                    broadcastWaxedState(target, false);

                    stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                    level.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.AXE_SCRAPE, SoundSource.PLAYERS, 1.0F, 1.0F);
                    spawnParticles(target, ParticleTypes.WAX_OFF, 20);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
                // 未打蜡：服务端不做动作，让原版处理
            });
            return;
        }
    }

    // 辅助方法：广播打蜡状态到所有追踪该实体的玩家
    private static void broadcastWaxedState(LivingEntity entity, boolean waxed) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            NetWorkManager.INSTANCE.send(
                    PacketDistributor.TRACKING_ENTITY.with(() -> entity),
                    new SyncWaxedPacket(entity.getId(), waxed)
            );
            // 同时发送给实体本身（防止自己没收到）
            if (entity instanceof ServerPlayer selfPlayer) {
                NetWorkManager.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> selfPlayer),
                        new SyncWaxedPacket(entity.getId(), waxed)
                );
            }
        }
    }

    private static boolean isMutantEnderman(LivingEntity entity) {
        return ModList.get().isLoaded("mutantmonsters") &&
                entity.getClass().getName().equals(MUTANT_ENDERMAN_CLASS);
    }

    private static boolean isMutantSnowGolem(LivingEntity entity) {
        return ModList.get().isLoaded("mutantmonsters") &&
                entity.getClass().getName().equals(MUTANT_SNOW_GOLEM_CLASS);
    }

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