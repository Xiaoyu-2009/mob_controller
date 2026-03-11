package net.xiaoyu.mob_controller.network;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.xiaoyu.mob_controller.capability.MobControlCapabilityProvider;

import java.util.function.Supplier;

public class MobControlCapabilitySyncPacket {
    private final int entityId;
    private final CompoundTag entityCap;

    public MobControlCapabilitySyncPacket(int entityId, CompoundTag entityCap) {
        this.entityId = entityId;
        this.entityCap = entityCap;
    }

    public MobControlCapabilitySyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.entityCap = buf.readAnySizeNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeNbt(this.entityCap);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
            return;
        }
        ctx.get().setPacketHandled(true);

        Player player = Minecraft.getInstance().player;

        if (player != null && player.level().getEntity(this.entityId) instanceof Mob mob) {
            mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY).ifPresent(cap -> cap.deserializeNBT(entityCap));
        }
    }
}
