package net.xiaoyu.mob_controller.network;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 客户端发送到服务端的“切换控制模式”数据包。
 *
 * <p>用于请求切换某个已被玩家控制生物的模式（跟随/停留/游荡）。</p>
 */

public record ToggleControlModePacket(int entityId) {
    /**
     * 从网络缓冲区反序列化数据包。
     *
     * @param buf 网络字节缓冲
     */
    public ToggleControlModePacket(FriendlyByteBuf buf) {
        this(buf.readInt());
    }

    /**
     * 将数据包编码到网络缓冲区。
     *
     * @param buf 网络字节缓冲
     */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId());
    }

    /**
     * 在服务端处理切换请求。
     *
     * <p>仅允许控制者本人切换自己受控生物的模式。</p>
     *
     * @param ctx 网络上下文提供器
     */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();

            if (player != null && player.level().getEntity(this.entityId()) instanceof Mob mob) {
                if (MobControlledData.isControlledEntity(mob) && Objects.equals(
                    MobControlledData.getControllerUUID(mob),
                    player.getUUID()
                )) {
                    if (MobControlUtil.isDirectRideableControlledMob(mob) && !player.isShiftKeyDown()) {
                        return;
                    }

                    MobControlledData.ControlMode newMode = MobControlledData.toggleControlMode(mob);

                    player.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);

                    String modeKey = "mob_controller.mode." + newMode.toString().toLowerCase();

                    if (Config.PLAY_SOUND_ON_MODE_SWITCH.get()) {
                        NetWorkManager.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new PlaySoundPacket());
                    }

                    MobControlUtil.showMessageToPlayer(player, mob.getDisplayName(), modeKey, new Object[]{}, ChatFormatting.GOLD);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
