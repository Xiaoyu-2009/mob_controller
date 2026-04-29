// ============================================================
// 源文件: C:/Users/Mnibr/Desktop/生物控制器源码/mob_controller-1.20.1-Forge/src\main\java\net\xiaoyu\mob_controller\mixin\MixinCreeperRespawn.java
// ============================================================

package net.xiaoyu.mob_controller.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 让被控制的苦力怕爆炸后也能进入重生队列。
 * 在 explodeCreeper 开头将苦力怕加入待重生列表，
 * 并主动向控制者发送复活倒计时消息。
 */
@Mixin(Creeper.class)
public abstract class MixinCreeperRespawn {

    @Inject(method = "explodeCreeper", at = @At("HEAD"))
    private void onExplodeCreeper(CallbackInfo ci) {
        Creeper creeper = (Creeper) (Object) this;
        if (!Config.ENABLE_RESPAWN.get()) {
            // 重生禁用时，仍然向控制者显示死因（不安排重生）
            if (MobControlledData.isControlledEntity(creeper) && creeper.level() instanceof ServerLevel serverLevel) {
                Player controller = MobControlledData.getController(creeper, serverLevel);
                if (controller instanceof ServerPlayer serverPlayer) {
                    String deathCause = Component.translatable("mob_controller.death.creeper_explode").getString();
                    serverPlayer.sendSystemMessage(Component.translatable("mob_controller.message.death_cause",
                            creeper.getDisplayName(), deathCause));
                }
            }
            return;
        }
        if (MobControlledData.isControlledEntity(creeper) && creeper.level() instanceof ServerLevel serverLevel) {
            // 构造死因（简单处理）
            String deathCause = Component.translatable("mob_controller.death.creeper_explode").getString();
            if (MobControlledData.scheduleRespawn(creeper, serverLevel, deathCause)) {
                Player controller = MobControlledData.getController(creeper, serverLevel);
                if (controller instanceof ServerPlayer serverPlayer) {
                    int seconds = Config.RESPAWN_DELAY_TICKS.get() / 20;
                    serverPlayer.sendSystemMessage(Component.translatable(
                            "mob_controller.message.respawn_scheduled_cause",
                            creeper.getDisplayName(),
                            deathCause,
                            seconds
                    ));
                }
            }
        }
    }
}