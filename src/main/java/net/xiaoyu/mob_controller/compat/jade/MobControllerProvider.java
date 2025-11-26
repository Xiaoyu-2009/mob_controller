package net.xiaoyu.mob_controller.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.xiaoyu.mob_controller.MobController;
import net.xiaoyu.mob_controller.util.MobControlledData;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

public class MobControllerProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {

	public static final MobControllerProvider INSTANCE = new MobControllerProvider();

	private MobControllerProvider() {}

	@Override
	public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
		if (accessor.getServerData().contains("MobControllerOwner")) {
			String ownerName = accessor.getServerData().getString("MobControllerOwner");
			tooltip.add(Component.translatable("jade.mob_owner", ownerName));
		}
	}

	@Override
	public void appendServerData(CompoundTag data, EntityAccessor accessor) {
		Entity entity = accessor.getEntity();
		if (!(entity instanceof Mob mob)) {
			return;
		}
		
		// 被控制的生物??
		if (!MobControlledData.isControlledMob(mob)) {
			return;
		}

		// 获取控制者
		Player controller = MobControlledData.getController(mob, accessor.getLevel());
		if (controller != null) {
			data.putString("MobControllerOwner", controller.getName().getString());
		}
	}

	@Override
	public ResourceLocation getUid() {
		return new ResourceLocation(MobController.MOD_ID, "mob_owner");
	}
}