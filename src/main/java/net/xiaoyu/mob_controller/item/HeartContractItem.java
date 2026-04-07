package net.xiaoyu.mob_controller.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.xiaoyu.mob_controller.util.MobControlledData;

import java.util.UUID;

public class HeartContractItem extends Item {
    public HeartContractItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Mob mob)) {
            return InteractionResult.PASS;
        }

        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!MobControlledData.isControlledEntity(mob)) {
            return InteractionResult.PASS;
        }

        UUID controllerUUID = MobControlledData.getControllerUUID(mob);
        if (controllerUUID == null || !controllerUUID.equals(player.getUUID())) {
            return InteractionResult.FAIL;
        }

        mob.setTarget(null);
        MobControlledData.releaseControl(mob);
        return InteractionResult.SUCCESS;
    }
}
