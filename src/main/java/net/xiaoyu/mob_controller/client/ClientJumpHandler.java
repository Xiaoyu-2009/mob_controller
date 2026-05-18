package net.xiaoyu.mob_controller.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xiaoyu.mob_controller.network.NetWorkManager;
import net.xiaoyu.mob_controller.network.PlayerJumpPacket;

@Mod.EventBusSubscriber(modid = "mob_controller", value = Dist.CLIENT)
public class ClientJumpHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        boolean isJumping = mc.options.keyJump.isDown();
        // 每 tick 发送当前跳跃状态（实现持续上升）
        NetWorkManager.INSTANCE.sendToServer(new PlayerJumpPacket(isJumping));
    }
}