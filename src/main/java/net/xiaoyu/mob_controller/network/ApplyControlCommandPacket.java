package net.xiaoyu.mob_controller.network;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.xiaoyu.mob_controller.item.MobControllerItem;
import net.xiaoyu.mob_controller.registry.ModItems;
import net.xiaoyu.mob_controller.registry.ModSounds;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;

import java.util.function.Supplier;

/**
 * 客户端发送到服务端的“控制令”数据包。
 *
 * <p>用于请求将玩家周围已控制生物批量切换到指定控制模式。</p>
 */

public record ApplyControlCommandPacket(MobControlledData.ControlMode mode) {
    /**
     * 从网络缓冲区反序列化数据包。
     *
     * @param buf 网络字节缓冲
     */
    public ApplyControlCommandPacket(FriendlyByteBuf buf) {
        this(MobControlledData.ControlMode.values()[buf.readVarInt()]);
    }

    /**
     * 将数据包内容写入网络缓冲区。
     *
     * @param buf 网络字节缓冲
     */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.mode().ordinal());
    }

    /**
     * 在服务端处理控制令请求。
     *
     * <p>仅当玩家主手持有“控制令”物品时生效，并在处理后向玩家反馈受影响生物数量。</p>
     *
     * @param ctx 网络上下文提供器
     */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !player.getMainHandItem().is(ModItems.CONTROL_COMMAND_ITEM.get())) {
                return;
            }

            int affectedCount = MobControllerItem.applyControlCommand(player, this.mode());
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.CONTROL_COMMAND_USE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.2f, 1.0f);
            String modeKey = "mob_controller.mode." + this.mode().toString().toLowerCase();
            MobControlUtil.showMessageToPlayer(
                    player,
                    Component.literal("[" + affectedCount + "]"),
                    modeKey,
                    new Object[]{},
                    ChatFormatting.GOLD
            );
        });
        ctx.get().setPacketHandled(true);
    }
}

