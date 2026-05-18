package net.xiaoyu.mob_controller.event;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.xiaoyu.mob_controller.MobController;
import net.xiaoyu.mob_controller.capability.ChilledCapability;
import net.xiaoyu.mob_controller.capability.ChilledCapabilityProvider;
import net.xiaoyu.mob_controller.network.NetWorkManager;
import net.xiaoyu.mob_controller.network.SyncChilledPacket;
import net.xiaoyu.mob_controller.network.client.ClientPacketHandler;

@Mod.EventBusSubscriber(modid = MobController.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChilledHandler {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof SnowGolem snowGolem)) return;

        Player player = event.getEntity();
        Level level = snowGolem.level();
        var hand = event.getHand();
        var stack = player.getItemInHand(hand);

        // 蓝冰：施加冷冻
        if (stack.is(Items.BLUE_ICE)) {
            if (level.isClientSide) {
                boolean chilled = ClientPacketHandler.isEntityChilled(snowGolem.getId());
                if (!chilled) {
                    player.swing(hand);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
                return;
            }

            snowGolem.getCapability(ChilledCapabilityProvider.CHILLED_CAPABILITY).ifPresent(cap -> {
                if (!cap.isChilled()) {
                    cap.setChilled(true);
                    broadcastChilledState(snowGolem, true);

                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                    // 播放放置雪的声音
                    level.playSound(null, snowGolem.getX(), snowGolem.getY(), snowGolem.getZ(),
                            SoundEvents.SNOW_PLACE, SoundSource.PLAYERS, 1.0F, 1.2F);
                    // 使用雪花粒子，数量适当增加
                    spawnParticles(snowGolem, ParticleTypes.SNOWFLAKE, 40);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
            });
            return;
        }

        // 刷子：移除冷冻
        if (stack.is(Items.BRUSH)) {
            if (level.isClientSide) {
                boolean chilled = ClientPacketHandler.isEntityChilled(snowGolem.getId());
                if (chilled) {
                    player.swing(hand);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
                return;
            }

            snowGolem.getCapability(ChilledCapabilityProvider.CHILLED_CAPABILITY).ifPresent(cap -> {
                if (cap.isChilled()) {
                    cap.setChilled(false);
                    broadcastChilledState(snowGolem, false);

                    stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                    level.playSound(null, snowGolem.getX(), snowGolem.getY(), snowGolem.getZ(),
                            SoundEvents.SNOW_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
                    // 使用烟雾粒子（类似刷子清扫）
                    spawnParticles(snowGolem, ParticleTypes.POOF, 25);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
            });
            return;
        }
    }

    private static void broadcastChilledState(SnowGolem golem, boolean chilled) {
        if (golem.level() instanceof ServerLevel serverLevel) {
            NetWorkManager.INSTANCE.send(
                    PacketDistributor.TRACKING_ENTITY.with(() -> golem),
                    new SyncChilledPacket(golem.getId(), chilled)
            );
            if (golem.getFirstPassenger() instanceof net.minecraft.server.level.ServerPlayer rider) {
                NetWorkManager.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> rider),
                        new SyncChilledPacket(golem.getId(), chilled)
                );
            }
        }
    }

    private static void spawnParticles(SnowGolem golem, net.minecraft.core.particles.SimpleParticleType particleType, int count) {
        Level level = golem.level();
        AABB box = golem.getBoundingBox();
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