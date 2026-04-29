package net.xiaoyu.mob_controller.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import net.xiaoyu.mob_controller.capability.MobControlCapabilityProvider;
import net.xiaoyu.mob_controller.network.MobControlCapabilitySyncPacket;
import net.xiaoyu.mob_controller.network.NetWorkManager;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * 护主切换器物品。
 * <p>用于切换受控生物的索敌模式（索敌/护主）。</p>
 * <p>使用方式：</p>
 * <ul>
 *   <li>左键/右键点击生物：切换单个生物的模式（左键=索敌，右键=护主）</li>
 *   <li>潜行+左键/右键：切换32格内所有受控生物的模式</li>
 * </ul>
 */
public class AggressiveSwitchItem extends Item {

    public AggressiveSwitchItem(Properties properties) {
        super(properties);
    }

    /**
     * 右键生物时触发单体切换（护主模式）
     */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Mob mob)) {
            return InteractionResult.PASS;
        }

        // 潜行时不处理单体（由鼠标事件中的批量逻辑处理）
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        // 仅在服务端执行逻辑
        if (player.level().isClientSide) {
            // 客户端播放手臂摆动动画
            player.swing(hand);
            return InteractionResult.SUCCESS;
        }

        // 检查是否为受控生物且控制者为当前玩家
        if (!MobControlledData.isControlledEntity(mob)) {
            player.displayClientMessage(Component.translatable("mob_controller.message.not_controlled").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        if (!player.getUUID().equals(MobControlledData.getControllerUUID(mob))) {
            player.displayClientMessage(Component.translatable("mob_controller.message.not_owner").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        // 右键 -> 护主模式 (false)
        MobControlledData.setSingleAggressiveMode(player, mob, false);

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("mob_controller.tooltip.aggressive_switch").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("mob_controller.tooltip.aggressive_switch.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("mob_controller.tooltip.aggressive_switch.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("mob_controller.tooltip.aggressive_switch.desc3").withStyle(ChatFormatting.GRAY));
    }
}