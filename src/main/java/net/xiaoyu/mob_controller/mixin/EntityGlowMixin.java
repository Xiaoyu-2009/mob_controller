package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.xiaoyu.mob_controller.item.LegionBannerItem;
import net.xiaoyu.mob_controller.network.client.ClientPacketHandler;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class EntityGlowMixin {

    @Shadow
    public abstract boolean isCurrentlyGlowing();

    /**
     * 军团模式下返回 true 启用轮廓（生物 + 玩家）。
     */
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void onIsCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;
        if (!(self instanceof Mob) && !(self instanceof Player)) {
            return;
        }
        if (!((AccessorLivingEntity) this).mob_controller$getEffectsDirty()) {
            if (self instanceof Mob mob && MobControlledData.isControlledEntity(mob)) {
                if (MobControlledData.isLegionMode(mob)) {
                    UUID controllerUUID = MobControlledData.getControllerUUID(mob);
                    if (controllerUUID != null && ClientPacketHandler.getLegionColorRGB(controllerUUID) != -1) {
                        cir.setReturnValue(true);
                    }
                }
            }
            else if (self instanceof Player player) {
                // 使用客户端缓存判断军团模式，而不是直接读NBT
                if (ClientPacketHandler.isPlayerInLegionMode(player.getUUID())) {
                    UUID playerUUID = player.getUUID();
                    int rgb = ClientPacketHandler.getLegionColorRGB(playerUUID);
                    if (rgb != -1) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }

    /**
     * 返回从客户端缓存中获取的队伍颜色（生物或玩家）。
     */
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void onGetTeamColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity)(Object)this;
        if (self instanceof Mob mob && MobControlledData.isControlledEntity(mob)) {
            if (MobControlledData.isLegionMode(mob)) {
                UUID controllerUUID = MobControlledData.getControllerUUID(mob);
                if (controllerUUID != null) {
                    int rgb = ClientPacketHandler.getLegionColorRGB(controllerUUID);
                    if (rgb != -1) {
                        cir.setReturnValue(rgb);
                    }
                }
            }
        }
        else if (self instanceof Player player) {
            if (ClientPacketHandler.isPlayerInLegionMode(player.getUUID())) {
                UUID playerUUID = player.getUUID();
                int rgb = ClientPacketHandler.getLegionColorRGB(playerUUID);
                if (rgb != -1) {
                    cir.setReturnValue(rgb);
                }
            }
        }
    }
}