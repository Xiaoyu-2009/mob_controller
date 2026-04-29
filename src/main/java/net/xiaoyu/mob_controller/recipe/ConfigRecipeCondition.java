package net.xiaoyu.mob_controller.recipe;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.MobController;

public class ConfigRecipeCondition implements ICondition {
    private final boolean expected;

    public ConfigRecipeCondition(boolean expected) {
        this.expected = expected;
    }

    @Override
    public ResourceLocation getID() {
        return new ResourceLocation(MobController.MOD_ID, "config_condition");
    }

    @Override
    public boolean test(ICondition.IContext context) {
        // 读取通用配置中的 use_grain_recipe 值
        return Config.USE_GRAIN_RECIPE.get() == expected;
    }

    public static class Serializer implements IConditionSerializer<ConfigRecipeCondition> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void write(JsonObject json, ConfigRecipeCondition value) {
            json.addProperty("expected", value.expected);
        }

        @Override
        public ConfigRecipeCondition read(JsonObject json) {
            return new ConfigRecipeCondition(json.get("expected").getAsBoolean());
        }

        @Override
        public ResourceLocation getID() {
            return new ResourceLocation(MobController.MOD_ID, "config_condition");
        }
    }
}