package net.xiaoyu.mob_controller.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.xiaoyu.mob_controller.client.GuiHandler;

import java.util.List;

public class MobArmor extends Item {
    public MobArmor(Item.Properties props) {
        super(props);
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level worldIn, BlockPos pos, Player player) {
        return !player.isCreative();
    }

    @Override
    public void appendHoverText(ItemStack stack, Level worldIn, List<Component> list, TooltipFlag flagIn) {
        list.add(Component.translatable("mob_controller.tooltip.armor").withStyle(ChatFormatting.AQUA));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof Mob mob && player instanceof ServerPlayer serverPlayer) {
            GuiHandler.openGuiArmor(serverPlayer, mob);
            return InteractionResult.SUCCESS;
        }
        
        return InteractionResult.PASS;
    }
}