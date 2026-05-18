package net.xiaoyu.mob_controller.compat.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.xiaoyu.mob_controller.MobController;
import net.xiaoyu.mob_controller.capability.WaxedCapability;
import net.xiaoyu.mob_controller.capability.WaxedCapabilityProvider;
import net.xiaoyu.mob_controller.item.LegionBannerItem;
import net.xiaoyu.mob_controller.util.MobControlledData;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class MobControllerProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {

    public static final MobControllerProvider INSTANCE = new MobControllerProvider();

    private MobControllerProvider() {}

    @Override
    public int getDefaultPriority() {
        return 0;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();

        // 控制者信息（生物）
        if (serverData.contains("MobControllerOwner")) {
            String ownerName = serverData.getString("MobControllerOwner");
            tooltip.add(Component.translatable("jade.mob_owner", ownerName));

            if (serverData.contains("MobControllerStatus")) {
                String statusKey = serverData.getString("MobControllerStatus");
                tooltip.add(Component.translatable(statusKey));
            }
            if (serverData.contains("MobControllerAggressive")) {
                boolean aggressive = serverData.getBoolean("MobControllerAggressive");
                tooltip.add(Component.translatable(aggressive ? "mob_controller.mode.aggressive" : "mob_controller.mode.protective"));
            }
            if (serverData.contains("LegionColor")) {
                String colorName = serverData.getString("LegionColor");
                ChatFormatting color = ChatFormatting.getByName(colorName);
                if (color == null) color = ChatFormatting.WHITE;
                Component teamName = LegionBannerItem.getTeamDisplayName(color);
                tooltip.add(Component.translatable("mob_controller.jade.legion", teamName).withStyle(color));
            }
        }

        // 打蜡信息
        if (serverData.getBoolean("Waxed")) {
            tooltip.add(Component.translatable("mob_controller.waxed"));
        }

        if (serverData.contains("PlayerLegionMode") && serverData.getBoolean("PlayerLegionMode")) {
            String colorName = serverData.getString("PlayerLegionColor");
            ChatFormatting color = ChatFormatting.getByName(colorName);
            if (color == null) color = ChatFormatting.WHITE;
            Component teamName = LegionBannerItem.getTeamDisplayName(color);
            tooltip.add(Component.translatable("mob_controller.jade.player_legion", teamName).withStyle(color));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        Entity entity = accessor.getEntity();
        // 生物数据（原有）
        if (entity instanceof Mob mob) {
            boolean waxed = mob.getCapability(WaxedCapabilityProvider.WAXED_CAPABILITY)
                    .map(WaxedCapability::isWaxed)
                    .orElse(false);
            data.putBoolean("Waxed", waxed);

            if (MobControlledData.isControlledEntity(mob)) {
                String controller = MobControlledData.getControllerName(mob, accessor.getLevel());
                if (controller != null) {
                    data.putString("MobControllerOwner", controller);
                }
                String statusKey = getControlModeTranslationKey(mob);
                if (statusKey != null) {
                    data.putString("MobControllerStatus", statusKey);
                }
                boolean aggressive = MobControlledData.isAggressiveMode(mob);
                data.putBoolean("MobControllerAggressive", aggressive);

                if (MobControlledData.isLegionMode(mob)) {
                    net.minecraft.world.entity.player.Player controllerPlayer = MobControlledData.getController(mob, accessor.getLevel());
                    if (controllerPlayer != null) {
                        ChatFormatting color = LegionBannerItem.getLegionColor(controllerPlayer);
                        data.putString("LegionColor", color.getName());
                    }
                }
            }
        }
        else if (entity instanceof Player player) {
            boolean legionMode = LegionBannerItem.isPlayerInLegionMode(player);
            data.putBoolean("PlayerLegionMode", legionMode);
            if (legionMode) {
                ChatFormatting color = LegionBannerItem.getLegionColor(player);
                data.putString("PlayerLegionColor", color.getName());
            }
        }
    }

    private static String getControlModeTranslationKey(Mob mob) {
        if (!MobControlledData.isControlledEntity(mob)) return null;
        MobControlledData.ControlMode mode = MobControlledData.getControlMode(mob);
        return "mob_controller.mode." + mode.toString().toLowerCase();
    }

    @Override
    public ResourceLocation getUid() {
        return new ResourceLocation(MobController.MOD_ID, "mob_owner");
    }
}