package net.xiaoyu.mob_controller.client.renderner;

import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IllagerRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.xiaoyu.mob_controller.entity.EntityControlledPillager;

/**
 * 受控掠夺者实体渲染器。
 */

@OnlyIn(Dist.CLIENT)
public class RendererControlledPillager extends IllagerRenderer<EntityControlledPillager> {
    /**
     * 掠夺者纹理。
     */
    private static final ResourceLocation PILLAGER = new ResourceLocation("textures/entity/illager/pillager.png");

    /**
     * 构造渲染器并附加手持物品图层。
     */
    public RendererControlledPillager(EntityRendererProvider.Context context) {
        super(context, new IllagerModel<>(context.bakeLayer(ModelLayers.PILLAGER)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    /**
     * 获取实体纹理。
     */
    @Override
    public ResourceLocation getTextureLocation(EntityControlledPillager entity) {
        return PILLAGER;
    }
}
