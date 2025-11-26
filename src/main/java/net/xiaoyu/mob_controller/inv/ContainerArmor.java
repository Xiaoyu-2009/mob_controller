package net.xiaoyu.mob_controller.inv;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.xiaoyu.mob_controller.MobController;
import net.xiaoyu.mob_controller.registry.ModMenuType;

public class ContainerArmor extends AbstractContainerMenu {
    private InventoryArmor inv;

    public ContainerArmor(int windowID, Inventory playerInv, FriendlyByteBuf buf) {
        this(windowID, playerInv, playerInv.player.level().getEntity(buf.readInt()));
    }

    public ContainerArmor(int windowID, Inventory playerInv, Entity e) {
        super(ModMenuType.ARMOR_MENU.get(), windowID);
        if (!(e instanceof Mob living)) return;
        this.inv = new InventoryArmor(living);
        this.inv.startOpen(playerInv.player);
        this.addSlot(new Slot(this.inv, 0, 80, 17) {

            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, new ResourceLocation(MobController.MOD_ID, "item/armor_slot_sword"));
            }
        });
        this.addSlot(new Slot(this.inv, 1, 80, 35) {

            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
            }
        });
        this.addSlot(new Slot(this.inv, 2, 44, 17) {

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET);
            }
        });
        this.addSlot(new Slot(this.inv, 3, 44, 35) {

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return ContainerArmor.this.inv.canPlaceItem(this.index, stack);
            }

            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE);
            }
        });
        this.addSlot(new Slot(this.inv, 4, 116, 17) {

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return ContainerArmor.this.inv.canPlaceItem(this.index, stack);
            }

            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS);
            }
        });
        this.addSlot(new Slot(this.inv, 5, 116, 35) {

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return ContainerArmor.this.inv.canPlaceItem(this.index, stack);
            }

            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS);
            }
        });
        for (int column = 0; column < 3; ++column) {
            for (int row = 0; row < 9; ++row) {
                this.addSlot(new Slot(playerInv, row + column * 9 + 9, 8 + row * 18, 66 + column * 18));
            }
        }

        for (int rowHotBar = 0; rowHotBar < 9; ++rowHotBar) {
            this.addSlot(new Slot(playerInv, rowHotBar, 8 + rowHotBar * 18, 124));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 6) {
                if (!this.moveItemStackTo(itemstack1, 6, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 6, true)) {
                return ItemStack.EMPTY;
            }
            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }
}