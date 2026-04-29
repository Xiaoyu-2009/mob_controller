package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.AngerManagement;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.nbt.CompoundTag;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;

@Mixin(Warden.class)
public abstract class WardenRespawnClearHateMixin {

    @Shadow
    private AngerManagement angerManagement;

    /**
     * 在 readAdditionalSaveData 之后，仅当监守者受控且为模组重生时才清除仇恨
     */
    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void onReadAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        Warden warden = (Warden) (Object) this;

        // 必须同时满足：受控状态 + 重生标记
        if (MobControlledData.isControlledEntity(warden) &&
                warden.getPersistentData().getBoolean("mob_controller:respawned")) {

            // 重置愤怒管理
            this.angerManagement = new AngerManagement(warden::canTargetEntity, Collections.emptyList());

            // 清除大脑仇恨记忆
            Brain<?> brain = warden.getBrain();
            if (brain != null) {
                brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
                brain.eraseMemory(MemoryModuleType.ROAR_TARGET);
            }

            // 清理标记
            warden.getPersistentData().remove("mob_controller:respawned");
        }
    }
}