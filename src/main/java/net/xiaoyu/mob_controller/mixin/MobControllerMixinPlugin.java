package net.xiaoyu.mob_controller.mixin;

import net.minecraftforge.fml.ModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MobControllerMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.equals("net.xiaoyu.mob_controller.mixin.alexsmobs.MixinCachalotWhaleSleep")) {
            return ModList.get().isLoaded("alexsmobs");
        }
        if (mixinClassName.equals("net.xiaoyu.mob_controller.mixin.alexscaves.MixinVesper")) {
            return ModList.get().isLoaded("alexscaves");
        }
        if (mixinClassName.equals("net.xiaoyu.mob_controller.mixin.cataclysm.MixinEnderGuardianNoRespawner")) {
            return ModList.get().isLoaded("cataclysm");
        }
        if (mixinClassName.equals("net.xiaoyu.mob_controller.mixin.aether.ValkyrieQueenMixin")) {
            return ModList.get().isLoaded("aether");
        }
        if (mixinClassName.equals("net.xiaoyu.mob_controller.mixin.twilightforest.AlphaYetiNoChestMixin")) {
            return isClassPresent("twilightforest.entity.boss.AlphaYeti");
        }
        if (mixinClassName.equals("net.xiaoyu.mob_controller.mixin.twilightforest.HydraNoChestMixin")) {
            return isClassPresent("twilightforest.entity.boss.Hydra");
        }
        if (mixinClassName.equals("net.xiaoyu.mob_controller.mixin.twilightforest.LichNoChestMixin")) {
            return isClassPresent("twilightforest.entity.boss.Lich");
        }
        if (mixinClassName.equals("net.xiaoyu.mob_controller.mixin.twilightforest.MinoshroomNoChestMixin")) {
            return isClassPresent("twilightforest.entity.boss.Minoshroom");
        }
        if (mixinClassName.equals("net.xiaoyu.mob_controller.mixin.twilightforest.NagaNoChestMixin")) {
            return isClassPresent("twilightforest.entity.boss.Naga");
        }
        if (mixinClassName.equals("net.xiaoyu.mob_controller.mixin.twilightforest.PlateauBossNoChestMixin")) {
            return isClassPresent("twilightforest.entity.boss.PlateauBoss");
        }
        if (mixinClassName.equals("net.xiaoyu.mob_controller.mixin.twilightforest.SnowQueenNoChestMixin")) {
            return isClassPresent("twilightforest.entity.boss.SnowQueen");
        }
        if (mixinClassName.equals("net.xiaoyu.mob_controller.mixin.twilightforest.UrGhastNoChestMixin")) {
            return isClassPresent("twilightforest.entity.boss.UrGhast");
        }
        return true;
    }

    private boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() { return null; }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}