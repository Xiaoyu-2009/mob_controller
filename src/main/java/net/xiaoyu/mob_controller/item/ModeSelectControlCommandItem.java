package net.xiaoyu.mob_controller.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.xiaoyu.mob_controller.util.MobControlledData;

import javax.annotation.Nullable;
import java.util.List;

public class ModeSelectControlCommandItem extends Item {

    private static final String TAG_SELECTED_MODE = "SelectedMode";

    public ModeSelectControlCommandItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        MobControlledData.ControlMode mode = getSelectedMode(stack);
        String modeKey = "mob_controller.mode." + mode.toString().toLowerCase();
        return Component.translatable("item.mob_controller.mode_select_control_command",
                Component.translatable(modeKey));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // 只播放动画，不发送任何数据包（避免双包冲突）
        if (level.isClientSide) {
            player.swing(hand);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("mob_controller.tooltip.mode_select_control_command").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("mob_controller.tooltip.mode_select_control_command.desc").withStyle(ChatFormatting.GRAY));
    }

    public static void cycleSelectedMode(ItemStack stack) {
        MobControlledData.ControlMode current = getSelectedMode(stack);
        int nextOrd = (current.ordinal() + 1) % MobControlledData.ControlMode.values().length;
        MobControlledData.ControlMode next = MobControlledData.ControlMode.values()[nextOrd];
        setSelectedMode(stack, next);
    }

    public static MobControlledData.ControlMode getSelectedMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_SELECTED_MODE)) {
            try {
                return MobControlledData.ControlMode.valueOf(tag.getString(TAG_SELECTED_MODE));
            } catch (IllegalArgumentException ignored) {}
        }
        return MobControlledData.ControlMode.FOLLOW;
    }

    public static void setSelectedMode(ItemStack stack, MobControlledData.ControlMode mode) {
        stack.getOrCreateTag().putString(TAG_SELECTED_MODE, mode.name());
    }
}