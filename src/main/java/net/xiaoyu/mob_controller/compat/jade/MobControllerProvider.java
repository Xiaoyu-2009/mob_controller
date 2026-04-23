package net.xiaoyu.mob_controller.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.xiaoyu.mob_controller.MobController;
import net.xiaoyu.mob_controller.util.MobControlledData;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade/WTHIT 实体信息提供器。
 *
 * <p>在提示框中展示受控生物的控制者名称，并由服务端下发所需数据。</p>
 */
public class MobControllerProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {

    /**
     * 提供器单例。
     */
    public static final MobControllerProvider INSTANCE = new MobControllerProvider();

    /**
     * 私有构造，使用单例。
     */
    private MobControllerProvider() {
    }

    /**
     * 在客户端提示框追加“控制者”信息。
     */
    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (accessor.getServerData().contains("MobControllerOwner")) {
            String ownerName = accessor.getServerData().getString("MobControllerOwner");
            if (!(accessor.getEntity() instanceof OwnableEntity)) {
                tooltip.add(Component.translatable("jade.mob_owner", ownerName));
            }
        }
    }

    /**
     * 在服务端写入提示框所需数据。
     */
    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        Entity entity = accessor.getEntity();
        if (!(entity instanceof Mob mob)) {
            return;
        }

        // 仅处理受控生物
        if (!MobControlledData.isControlledEntity(mob)) {
            return;
        }

        // 写入控制者名称
        String controller = MobControlledData.getControllerName(mob, accessor.getLevel());
        if (controller != null) {
            data.putString("MobControllerOwner", controller);
        }
    }

    /**
     * 获取该提供器的唯一标识。
     */
    @Override
    public ResourceLocation getUid() {
        return new ResourceLocation(MobController.MOD_ID, "mob_owner");
    }
}
