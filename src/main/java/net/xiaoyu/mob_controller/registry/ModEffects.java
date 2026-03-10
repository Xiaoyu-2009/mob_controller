package net.xiaoyu.mob_controller.registry;

import net.minecraft.world.effect.InstantenousMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.xiaoyu.mob_controller.MobController;
import org.jetbrains.annotations.Nullable;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MobController.MOD_ID);
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, MobController.MOD_ID);

    public static final RegistryObject<MobEffect> SPECIAL_INSTANT_HEALTH = MOB_EFFECTS.register("special_instant_health", () ->
            new InstantenousMobEffect(MobEffectCategory.BENEFICIAL, 0xF82423) {
                @Override
                public void applyInstantenousEffect(@Nullable Entity source, @Nullable Entity indirectSource, LivingEntity livingEntity,
                                                    int amplifier, double health) {
                    livingEntity.heal((float) (health * (4 << amplifier) + 0.5D));
                }
            });

    public static final RegistryObject<Potion> SPECIAL_HEALING = POTIONS.register("special_healing", () -> new Potion(new MobEffectInstance(SPECIAL_INSTANT_HEALTH.get(), 1)));
}
