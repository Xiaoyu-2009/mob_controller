package net.xiaoyu.mob_controller.compat.jade;

import net.minecraft.world.entity.Entity;
import snownee.jade.api.*;

@WailaPlugin
public class MobControllerPlugin implements IWailaPlugin {

	@Override
	public void register(IWailaCommonRegistration registration) {
		registration.registerEntityDataProvider(MobControllerProvider.INSTANCE, Entity.class);
	}

	@Override
	public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(MobControllerProvider.INSTANCE, Entity.class);
		/* registration.registerEntityComponent(MobControllerNameProvider.INSTANCE, Entity.class); */
	}
}