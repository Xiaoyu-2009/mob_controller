package net.xiaoyu.mob_controller.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.xiaoyu.mob_controller.network.client.ClientPacketHandler;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 服务端下发到客户端的控制能力同步数据包。
 *
 * <p>用于将指定实体的 {@link net.xiaoyu.mob_controller.capability.MobControlCapability} 数据
 * 同步到客户端实体副本。</p>
 */
public record MobControlCapabilitySyncPacket(int entityId, CompoundTag entityCap) {
    /**
     * 从网络缓冲区反序列化数据包。
     *
     * @param buf 网络字节缓冲
     */
    public MobControlCapabilitySyncPacket(FriendlyByteBuf buf) {
        this(buf.readInt(), Objects.requireNonNull(buf.readAnySizeNbt()));
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
        ctx.get().enqueueWork(() -> ClientPacketHandler.handleMobControlCapabilitySync(ctx, this));
    }
}
