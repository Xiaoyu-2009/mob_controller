package net.xiaoyu.mob_controller.advancement;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.xiaoyu.mob_controller.MobController;

public class ReleaseControlTrigger extends SimpleCriterionTrigger<ReleaseControlTrigger.Instance> {
    private static final ResourceLocation ID = MobController.location("release_control");

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    protected Instance createInstance(JsonObject json, ContextAwarePredicate predicate, DeserializationContext context) {
        return new Instance(predicate);
    }

    public void trigger(ServerPlayer player, Mob releasedMob) {
        this.trigger(player, instance -> true);
    }

    public static class Instance extends AbstractCriterionTriggerInstance {
        public Instance(ContextAwarePredicate predicate) {
            super(ID, predicate);
        }
    }
}