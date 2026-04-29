package net.xiaoyu.mob_controller.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.xiaoyu.mob_controller.capability.WaxedCapability;
import net.xiaoyu.mob_controller.capability.WaxedCapabilityProvider;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import net.xiaoyu.mob_controller.MobController;
import net.xiaoyu.mob_controller.util.MobControlledData;

/**
 * Jade/WTHIT 实体信息提供器。
 *
 * <p>在提示框中展示受控生物的控制者名称，以及当前模式（护主/索敌）。</p>
 * <p>注意：移除原版主人显示（避免重复）的工作已交由 {@link MobControllerOwnerRemover} 处理，</p>
 * <p>本类不再负责移除操作，仅专注于添加控制信息。</p>
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
     * 使用默认优先级（0），确保控制信息能够尽快显示，不被其它高优先级组件延迟。
     *
     * @return 0（默认优先级）
     */
    @Override
    public int getDefaultPriority() {
        return 0;
    }

    /**
     * 在客户端提示框追加“控制者”信息和当前状态。
     *
     * @param tooltip  提示框构建器
     * @param accessor 实体访问器
     * @param config   插件配置
     */
    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (accessor.getServerData().contains("MobControllerOwner")) {
            String ownerName = accessor.getServerData().getString("MobControllerOwner");
            tooltip.add(Component.translatable("jade.mob_owner", ownerName));

            if (accessor.getServerData().contains("MobControllerStatus")) {
                String statusKey = accessor.getServerData().getString("MobControllerStatus");
                tooltip.add(Component.translatable(statusKey));
            }
            if (accessor.getServerData().contains("MobControllerAggressive")) {
                boolean aggressive = accessor.getServerData().getBoolean("MobControllerAggressive");
                tooltip.add(Component.translatable(aggressive ? "mob_controller.mode.aggressive" : "mob_controller.mode.protective"));
            }
        }
        if (accessor.getServerData().getBoolean("Waxed")) {
            tooltip.add(Component.translatable("mob_controller.waxed"));
        }
    }

    /**
     * 在服务端写入提示框所需数据。
     *
     * @param data     用于携带数据的 NBT 标签
     * @param accessor 实体访问器
     */
    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        Entity entity = accessor.getEntity();
        if (!(entity instanceof Mob mob)) {
            return;
        }

        boolean waxed = mob.getCapability(WaxedCapabilityProvider.WAXED_CAPABILITY)
                .map(WaxedCapability::isWaxed)
                .orElse(false);
        data.putBoolean("Waxed", waxed);

        if (!MobControlledData.isControlledEntity(mob)) {
            return;
        }

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
    }

    /**
     * 获取生物当前控制模式的翻译键。
     *
     * @param mob 目标生物
     * @return 翻译键（例如："mob_controller.mode.follow"），若未受控则返回 null
     */
    private static String getControlModeTranslationKey(Mob mob) {
        if (!MobControlledData.isControlledEntity(mob)) {
            return null;
        }
        MobControlledData.ControlMode mode = MobControlledData.getControlMode(mob);
        return "mob_controller.mode." + mode.toString().toLowerCase();
    }

    /**
     * 获取该提供器的唯一标识。
     *
     * @return 唯一标识 ResourceLocation
     */
    @Override
    public ResourceLocation getUid() {
        return new ResourceLocation(MobController.MOD_ID, "mob_owner");
    }
}