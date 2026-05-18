// net.xiaoyu.mob_controller.advancement.FirstCompanionTrigger.java
package net.xiaoyu.mob_controller.advancement;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.xiaoyu.mob_controller.MobController;

public class FirstCompanionTrigger extends SimpleCriterionTrigger<FirstCompanionTrigger.Instance> {
    private static final ResourceLocation ID = MobController.location("first_companion");

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    protected Instance createInstance(JsonObject json, ContextAwarePredicate predicate, DeserializationContext context) {
        return new Instance(predicate);
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, instance -> true);
    }

    public static class Instance extends AbstractCriterionTriggerInstance {
        public Instance(ContextAwarePredicate predicate) {
            super(ID, predicate);
        }
    }
}