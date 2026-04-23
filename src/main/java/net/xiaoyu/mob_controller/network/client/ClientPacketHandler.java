package net.xiaoyu.mob_controller.network.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.xiaoyu.mob_controller.capability.MobControlCapabilityProvider;
import net.xiaoyu.mob_controller.network.MobControlCapabilitySyncPacket;

import java.util.function.Supplier;

public class ClientPacketHandler {
    public static void handleMobControlCapabilitySync(Supplier<NetworkEvent.Context> ctx, MobControlCapabilitySyncPacket packet) {
        if (ctx.get().getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
            return;
        }
        ctx.get().setPacketHandled(true);

        Player player = Minecraft.getInstance().player;

        if (player != null && player.level().getEntity(packet.entityId()) instanceof Mob mob) {
            mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY).ifPresent(cap -> cap.deserializeNBT(packet.entityCap()));
        }
    }
}
