package net.xiaoyu.mob_controller.network;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.xiaoyu.mob_controller.capability.MobControlCapabilityProvider;

import java.util.Objects;
import java.util.function.Supplier;
/**
 * 服务端下发到客户端的控制能力同步数据包。
 *
 * <p>用于将指定实体的 {@link net.xiaoyu.mob_controller.capability.MobControlCapability} 数据
 * 同步到客户端实体副本。</p>
 */
public class MobControlCapabilitySyncPacket {
    /** 需要同步的实体 ID。 */
    private final int entityId;
    /** 对应实体的能力 NBT 数据。 */
    private final CompoundTag entityCap;

    /**
     * 构造同步数据包。
     *
     * @param entityId 目标实体 ID
     * @param entityCap 能力 NBT
     */
    public MobControlCapabilitySyncPacket(int entityId, CompoundTag entityCap) {
        this.entityId = entityId;
        this.entityCap = entityCap;
    }

    /**
     * 从网络缓冲区反序列化数据包。
     *
     * @param buf 网络字节缓冲
     */
    public MobControlCapabilitySyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.entityCap = Objects.requireNonNull(buf.readAnySizeNbt());
    }

    /**
     * 将数据包编码到网络缓冲区。
     *
     * @param buf 网络字节缓冲
     */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeNbt(this.entityCap);
    }

    /**
     * 在客户端线程应用能力同步。
     *
     * @param ctx 网络上下文提供器
     */
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
