package net.xiaoyu.mob_controller.inv;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
/**
 * 受控生物装备库存。
 *
 * <p>将菜单索引映射到生物装备槽位，并在服务端同步写回实体。</p>
 */
public class InventoryArmor extends SimpleContainer {
    /** 菜单槽位与生物装备槽位映射。 */
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
            EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /** 被编辑装备的目标生物。 */
    private final Mob mob;

    /**
     * 使用目标生物初始化装备库存。
     *
     * @param living 目标生物
     */
    public InventoryArmor(Mob living) {
        super(6);
        this.mob = living;
        for (int x = 0; x < 6; x++) {
            ItemStack stack = living.getItemBySlot(SLOTS[x]);
            super.setItem(x, stack);
        }
    }

    /**
     * 设置槽位物品，并在服务端写回生物装备槽。
     */
    @Override
    public void setItem(int index, ItemStack stack) {
        super.setItem(index, stack);
        EquipmentSlot slot = this.slotType(index);
        if (slot != null && !this.mob.level().isClientSide) {
            this.mob.setItemSlot(slot, stack);
        }
    }

    /**
     * 校验指定槽位是否允许放入物品。
     */
    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        EquipmentSlot slot = this.slotType(index);
        if (slot == null) {
            return false;
        }

        return switch (slot) {
            case CHEST, LEGS, FEET -> stack.canEquip(slot, this.mob);
            default -> true;
        };
    }

    /**
     * 将菜单索引转换为对应装备槽位。
     *
     * @param index 菜单槽位索引
     * @return 对应装备槽位，越界时为 {@code null}
     */
    @Nullable
    public EquipmentSlot slotType(int index) {
        if (index < 0 || index >= SLOTS.length) {
            return null;
        }

        return SLOTS[index];
    }
}
