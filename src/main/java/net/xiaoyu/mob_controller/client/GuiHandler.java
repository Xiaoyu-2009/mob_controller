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

public class GuiHandler {
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