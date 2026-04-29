package net.xiaoyu.mob_controller.compat.jade;

import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.Identifiers;
import snownee.jade.api.config.IPluginConfig;
import net.xiaoyu.mob_controller.MobController;

/**
 * 专用于移除原版主人显示（AnimalOwnerProvider）的组件。
 *
 * <p>该组件优先级设为 10，确保在 AnimalOwnerProvider（默认优先级0）添加主人行之后立即将其移除，</p>
 * <p>从而避免受控生物同时显示“原版主人”和“mob_controller 控制者”，造成信息重复。</p>
 * <p>注意：该组件只负责移除，不添加任何额外内容。</p>
 */
public class MobControllerOwnerRemover implements IEntityComponentProvider {

    /**
     * 单例实例。
     */
    public static final MobControllerOwnerRemover INSTANCE = new MobControllerOwnerRemover();

    /**
     * 私有构造，使用单例。
     */
    private MobControllerOwnerRemover() {
    }

    /**
     * 提升优先级至 10，确保在原版 AnimalOwnerProvider（优先级0）之后执行移除操作。
     *
     * @return 10
     */
    @Override
    public int getDefaultPriority() {
        return 10;
    }

    /**
     * 当目标实体为受控生物时，从提示框中移除原版添加的主人行。
     *
     * @param tooltip  提示框构建器
     * @param accessor 实体访问器（包含服务端下发的数据）
     * @param config   插件配置
     */
    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        // 仅当实体是受控生物时才进行移除（通过检查服务端数据中是否含有控制者标记）
        if (accessor.getServerData().contains("MobControllerOwner")) {
            tooltip.remove(Identifiers.MC_ANIMAL_OWNER);
        }
    }

    /**
     * 获取该组件的唯一标识。
     *
     * @return 唯一标识 ResourceLocation
     */
    @Override
    public ResourceLocation getUid() {
        return new ResourceLocation(MobController.MOD_ID, "owner_remover");
    }
}