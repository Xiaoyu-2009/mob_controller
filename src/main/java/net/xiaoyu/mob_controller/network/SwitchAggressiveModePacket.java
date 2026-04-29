package net.xiaoyu.mob_controller.network;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.NetworkEvent;
import net.xiaoyu.mob_controller.registry.ModItems;
import net.xiaoyu.mob_controller.registry.ModSounds;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 护主/索敌模式切换数据包
 * @param aggressive true=索敌模式, false=护主模式
 * @param targetEntityId 目标实体ID，-1表示批量切换，-2表示无效单体（仅用于反馈），>=0表示单体切换的目标
 */
public record SwitchAggressiveModePacket(boolean aggressive, int targetEntityId) {

    public SwitchAggressiveModePacket(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(aggressive);
        buf.writeInt(targetEntityId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 检查手持物品是否为护主切换器
            if (!player.getMainHandItem().is(ModItems.AGGRESSIVE_SWITCH_ITEM.get())) return;

            // 批量切换模式
            if (targetEntityId == -1) {
                int affectedCount = MobControlledData.setAggressiveModeForAll(player, 32, aggressive);
                if (affectedCount == 0) {
                    player.displayClientMessage(Component.translatable("mob_controller.message.no_controlled_mobs_nearby")
                            .withStyle(ChatFormatting.RED), true);
                    return;
                }
                // 🎵 批量切换成功时播放声音
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.AGGRESSIVE_SWITCH_BATCH.get(), net.minecraft.sounds.SoundSource.PLAYERS, 2.0f, 1.0f);
                String modeKey = aggressive ? "mob_controller.mode.aggressive" : "mob_controller.mode.protective";
                MobControlUtil.showMessageToPlayer(player, Component.literal("[" + affectedCount + "]"),
                        modeKey, new Object[]{}, ChatFormatting.GOLD);
                return;
            }

            // 无效单体（没有瞄准有效生物）
            if (targetEntityId == -2) {
                player.displayClientMessage(Component.translatable("mob_controller.message.no_controlled_mob_target")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }

            // 单体切换模式
            if (player.level().getEntity(targetEntityId) instanceof Mob mob) {
                // 验证控制权
                if (!MobControlledData.isControlledEntity(mob) ||
                        !Objects.equals(MobControlledData.getControllerUUID(mob), player.getUUID())) {
                    player.displayClientMessage(Component.translatable("mob_controller.message.not_owner")
                            .withStyle(ChatFormatting.RED), true);
                    return;
                }

                // 执行单体切换
                MobControlledData.setSingleAggressiveMode(player, mob, aggressive);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}