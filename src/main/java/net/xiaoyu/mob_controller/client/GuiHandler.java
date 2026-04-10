package net.xiaoyu.mob_controller.client;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkHooks;
import net.xiaoyu.mob_controller.inv.ContainerArmor;
import org.jetbrains.annotations.NotNull;
/**
 * 生物装备界面打开辅助类。
 */
public class GuiHandler {
    /**
     * 为指定玩家打开目标生物的装备编辑界面。
     *
     * @param player 打开界面的服务端玩家
     * @param living 目标生物
     */
    public static void openGuiArmor(ServerPlayer player, Mob living) {
        NetworkHooks.openScreen(
                player, new MenuProvider() {
                    @Override
                    public @NotNull Component getDisplayName() {
                        return living.getName();
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int i, @NotNull Inventory arg, @NotNull Player arg2) {
                        return new ContainerArmor(i, arg, living);
                    }
                }, buf -> buf.writeInt(living.getId())
        );
    }
}
