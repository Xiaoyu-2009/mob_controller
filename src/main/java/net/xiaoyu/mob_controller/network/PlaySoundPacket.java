package net.xiaoyu.mob_controller.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.network.NetworkEvent;
import net.xiaoyu.mob_controller.network.client.ClientPacketHandler;

import java.util.function.Supplier;

/**
 * 客户端播放声音的数据包（仅单玩家）。
 */
public record PlaySoundPacket() {
    public PlaySoundPacket(FriendlyByteBuf buf) {
        this();
    }

    public void toBytes(FriendlyByteBuf buf) {
        // 无需写入数据
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientPacketHandler.handlePlaySound(ctx));
        ctx.get().setPacketHandled(true);
    }
}