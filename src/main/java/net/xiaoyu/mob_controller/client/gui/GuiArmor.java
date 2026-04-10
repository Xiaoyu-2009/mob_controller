package net.xiaoyu.mob_controller.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.xiaoyu.mob_controller.MobController;
import net.xiaoyu.mob_controller.inv.ContainerArmor;

/**
 * 受控生物装备编辑界面。
 *
 * <p>用于展示并编辑目标生物的主副手与护甲槽位。</p>
 */
public class GuiArmor extends AbstractContainerScreen<ContainerArmor> {
    /**
     * 界面背景纹理。
     */
    private static final ResourceLocation armorGui = new ResourceLocation(MobController.MOD_ID, "textures/gui/armor.png");
    /**
     * 菜单标题文本。
     */
    private final Component chatComponent;

    /**
     * 构造装备界面。
     *
     * @param container 菜单容器
     * @param playerInv 玩家背包
     * @param title     界面标题
     */
    public GuiArmor(ContainerArmor container, Inventory playerInv, Component title) {
        super(container, playerInv, title);
        this.chatComponent = title;
    }

    /**
     * 渲染界面与工具提示。
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    /**
     * 渲染标题文本。
     */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(
            this.font, this.chatComponent, this.imageWidth / 2 - this.font.width(this.chatComponent)
                                                                 / 2, 6, 4210752, false
        );
    }

    /**
     * 渲染背景纹理。
     */
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        graphics.blit(armorGui, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }
}
