package net.xiaoyu.mob_controller.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;

import javax.annotation.Nullable;
import java.util.List;

public class CreativeMobControllerItem extends MobControllerItem {

    public CreativeMobControllerItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Mob mob)) return InteractionResult.PASS;
        if (MobControlledData.isControlledEntity(mob)) return InteractionResult.PASS;

        Level level = player.level();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        // 黑名单 / 已有主人
        if (Config.BLACKLISTED_MOBS.get().contains(EntityType.getKey(mob.getType()).toString()) || hasOwnerOrTameTag(mob)) {
            spawnParticles(mob, false);
            return InteractionResult.FAIL;
        }
        // 高生命值同类限制
        if (MobControlledData.hasPlayerControlledSameHighHealthMob(player.getUUID(), mob)) {
            spawnParticles(mob, false);
            return InteractionResult.FAIL;
        }

        mob.setTarget(null);
        controlMob(player, mob);
        MobControlUtil.showMessageToPlayer(player, mob.getDisplayName(), "mob_controller.mode.follow", new Object[]{}, ChatFormatting.GOLD);
        spawnParticles(mob, true);
        return InteractionResult.SUCCESS;
    }

    // 禁用饮用
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    private boolean hasOwnerOrTameTag(Mob mob) {
        if (mob instanceof net.minecraft.world.entity.TamableAnimal tamable && tamable.isTame()) return true;
        net.minecraft.nbt.CompoundTag nbt = mob.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        return nbt.contains("Owner") || nbt.contains("OwnerUUID") || (nbt.contains("Tame") && nbt.getBoolean("Tame"));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("mob_controller.tooltip.creative_mob_controller").withStyle(ChatFormatting.GOLD));
    }
}