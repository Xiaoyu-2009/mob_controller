package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 袭击（Raid）类的 Mixin，用于阻止受控灾厄村民加入袭击。
 */
@Mixin(Raid.class)
public class RaidMixin {

    /**
     * 在 joinRaid 方法头部注入：若掠夺者已被控制，则直接返回，不执行后续的添加逻辑。
     *
     * @param group    袭击波次编号
     * @param raider   要加入的掠夺者
     * @param pos      生成位置（可为 null）
     * @param p_37717_ 标记
     * @param ci       回调信息
     */
    @Inject(method = "joinRaid", at = @At("HEAD"), cancellable = true)
    private void onJoinRaid(int group, Raider raider, net.minecraft.core.BlockPos pos, boolean p_37717_, CallbackInfo ci) {
        if (MobControlledData.isControlledEntity(raider)) {
            ci.cancel(); // 阻止受控生物加入袭击
        }
    }
}