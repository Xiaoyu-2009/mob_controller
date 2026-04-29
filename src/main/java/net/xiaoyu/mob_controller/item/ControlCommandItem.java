// 文件：net/xiaoyu/mob_controller/item/ControlCommandItem.java
package net.xiaoyu.mob_controller.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class ControlCommandItem extends Item {
    public ControlCommandItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("mob_controller.tooltip.control_command.range").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("mob_controller.tooltip.control_command.left").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("mob_controller.tooltip.control_command.middle").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("mob_controller.tooltip.control_command.right").withStyle(ChatFormatting.GRAY));
    }
}