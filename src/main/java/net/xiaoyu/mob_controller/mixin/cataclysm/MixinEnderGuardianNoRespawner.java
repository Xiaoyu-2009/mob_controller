package net.xiaoyu.mob_controller.mixin.cataclysm;

import com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.Ender_Guardian_Entity;
import net.minecraft.world.entity.LivingEntity;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * 混入末影守卫，阻止被 mob_controller 控制的末影守卫在死后生成 Boss 重生笼。
 * 该 Mixin 仅在 Cataclysm 模组加载时生效。
 */
@Mixin(Ender_Guardian_Entity.class)
public class MixinEnderGuardianNoRespawner {

    /**
     * 在 AfterDefeatBoss 方法开头注入，如果守卫已被控制则直接返回，不执行 Respawner。
     *
     * @param living 击败者（可能为 null）
     * @param ci     回调信息
     */
    @Inject(method = "AfterDefeatBoss", at = @At("HEAD"), cancellable = true, remap = false)
    private void onAfterDefeatBoss(@Nullable LivingEntity living, CallbackInfo ci) {
        Ender_Guardian_Entity guardian = (Ender_Guardian_Entity) (Object) this;
        // 检查是否被 mob_controller 控制
        if (Config.ENABLE_RESPAWN.get() && MobControlledData.isControlledEntity(guardian)) {
            ci.cancel(); // 跳过重生笼生成
        }
    }
}