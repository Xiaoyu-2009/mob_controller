package net.xiaoyu.mob_controller.client.renderner;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.WitchModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.WitchItemLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.xiaoyu.mob_controller.entity.EntityControlledWitch;
/**
 * 受控女巫实体渲染器。
 */
@OnlyIn(Dist.CLIENT)
public class RendererControlledWitch extends MobRenderer<EntityControlledWitch, WitchModel<EntityControlledWitch>> {
    /** 女巫纹理。 */
    private static final ResourceLocation WITCH_LOCATION = new ResourceLocation("textures/entity/witch.png");

    /**
     * 构造渲染器并附加女巫手持物品图层。
     */
    public RendererControlledWitch(EntityRendererProvider.Context context) {
        super(context, new WitchModel<>(context.bakeLayer(ModelLayers.WITCH)), 0.5F);
        this.addLayer(new WitchItemLayer<>(this, context.getItemInHandRenderer()));
    }

    /** 根据手持状态更新模型后执行渲染。 */
    @Override
    public void render(EntityControlledWitch entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        this.model.setHoldingItem(!entity.getMainHandItem().isEmpty());
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** 获取实体纹理。 */
    @Override
    public ResourceLocation getTextureLocation(EntityControlledWitch entity) {
        return WITCH_LOCATION;
    }

    /** 调整模型缩放。 */
    @Override
    protected void scale(EntityControlledWitch livingEntity, PoseStack poseStack, float partialTickTime) {
        float f = 0.9375F;
        poseStack.scale(f, f, f);
    }
}
