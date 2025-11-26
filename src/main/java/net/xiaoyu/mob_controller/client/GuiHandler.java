package net.xiaoyu.mob_controller.client;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkHooks;
import net.xiaoyu.mob_controller.inv.ContainerArmor;

public class GuiHandler {
    public static void openGuiArmor(ServerPlayer player, Mob living) {
        NetworkHooks.openScreen(
            player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                return living.getName();
                }

                @Override
                public AbstractContainerMenu createMenu(int i, Inventory arg, Player arg2) {
                    return new ContainerArmor(i, arg, living);
                }
            }, buf -> buf.writeInt(living.getId())
        );
    }
}