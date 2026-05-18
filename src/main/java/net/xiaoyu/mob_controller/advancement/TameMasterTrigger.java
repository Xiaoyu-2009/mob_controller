package net.xiaoyu.mob_controller.advancement;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.common.util.FakePlayer;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.MobController;
import net.xiaoyu.mob_controller.util.MobControlledData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TameMasterTrigger extends SimpleCriterionTrigger<TameMasterTrigger.Instance> {
    private static final ResourceLocation ID = MobController.location("tame_master");
    // 玩家数据存储键
    private static final String TAMED_TYPES_KEY = "mob_controller_tamed_types";
    private static final String HAD_HIGH_HEALTH_KEY = "mob_controller_had_high_health";

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    protected Instance createInstance(JsonObject json, ContextAwarePredicate predicate, DeserializationContext context) {
        return new Instance(predicate);
    }

    public void trigger(ServerPlayer player, Mob tamedMob) {
        if (player instanceof FakePlayer) return;
        CompoundTag data = player.getPersistentData();
        Set<String> tamedTypes = getTamedTypesSet(data);
        String typeKey = EntityType.getKey(tamedMob.getType()).toString();
        if (tamedTypes.add(typeKey)) {
            // 新种类，保存回NBT
            saveTamedTypesSet(data, tamedTypes);
            // 检查是否包含高血量生物
            boolean hadHighHealth = data.getBoolean(HAD_HIGH_HEALTH_KEY);
            if (!hadHighHealth && MobControlledData.isHighHealthMob(tamedMob)) {
                data.putBoolean(HAD_HIGH_HEALTH_KEY, true);
                hadHighHealth = true;
            }
            // 判断条件是否达成
            if (tamedTypes.size() >= 20 && hadHighHealth) {
                this.trigger(player, instance -> true);
            }
        }
    }

    private Set<String> getTamedTypesSet(CompoundTag data) {
        Set<String> set = new HashSet<>();
        String[] list = data.getString(TAMED_TYPES_KEY).split(",");
        for (String s : list) {
            if (!s.isEmpty()) set.add(s);
        }
        return set;
    }

    private void saveTamedTypesSet(CompoundTag data, Set<String> set) {
        data.putString(TAMED_TYPES_KEY, String.join(",", set));
    }

    public static class Instance extends AbstractCriterionTriggerInstance {
        public Instance(ContextAwarePredicate predicate) {
            super(ID, predicate);
        }
    }
}