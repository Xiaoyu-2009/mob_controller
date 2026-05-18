package net.xiaoyu.mob_controller.advancement;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.xiaoyu.mob_controller.MobController;
import net.xiaoyu.mob_controller.registry.ModItems;

public class CraftControllerTrigger extends SimpleCriterionTrigger<CraftControllerTrigger.Instance> {
    private static final ResourceLocation ID = MobController.location("craft_controller");

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    protected Instance createInstance(JsonObject json, ContextAwarePredicate predicate, DeserializationContext context) {
        return new Instance(predicate);
    }

    public void trigger(ServerPlayer player, ItemStack crafted) {
        if (crafted.is(ModItems.MOB_CONTROLLER_ITEM.get()) || crafted.is(ModItems.GRAIN_ITEM.get())) {
            this.trigger(player, instance -> true);
        }
    }

    public static class Instance extends AbstractCriterionTriggerInstance {
        public Instance(ContextAwarePredicate predicate) {
            super(ID, predicate);
        }
    }
}