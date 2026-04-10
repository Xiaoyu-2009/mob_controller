package net.xiaoyu.mob_controller.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.xiaoyu.mob_controller.client.GuiHandler;
import net.xiaoyu.mob_controller.util.MobControlledData;

import java.util.List;
import javax.annotation.Nullable;

/**
 * 盔甲编辑蓝图物品。
 *
 * <p>可对受控生物打开装备编辑界面，用于调整主副手与护甲槽位。</p>
 */
public class MobArmor extends Item {
    /**
     * 构造盔甲编辑蓝图物品。
     *
     * @param props 物品属性
     */
    public MobArmor(Item.Properties props) {
        super(props);
    }

    /**
     * 禁止创造模式玩家以外的方块攻击破坏行为。
     */
    @Override
    public boolean canAttackBlock(BlockState state, Level worldIn, BlockPos pos, Player player) {
        return !player.isCreative();
    }

    /**
     * 对受控生物使用时打开装备编辑菜单。
     */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof Mob mob && MobControlledData.isControlledEntity(mob) && player instanceof ServerPlayer serverPlayer) {
            GuiHandler.openGuiArmor(serverPlayer, mob);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    /**
     * 添加物品提示文本。
     */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> list, TooltipFlag flagIn) {
        list.add(Component.translatable("mob_controller.tooltip.armor").withStyle(ChatFormatting.AQUA));
    }
}
