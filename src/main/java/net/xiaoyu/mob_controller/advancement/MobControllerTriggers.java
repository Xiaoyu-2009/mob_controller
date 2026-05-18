// net.xiaoyu.mob_controller.advancement.MobControllerTriggers.java
package net.xiaoyu.mob_controller.advancement;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.xiaoyu.mob_controller.MobController;

public class MobControllerTriggers {
    public static final CraftControllerTrigger CRAFT_CONTROLLER = new CraftControllerTrigger();
    public static final UseControlCommandTrigger USE_CONTROL_COMMAND = new UseControlCommandTrigger();
    public static final TameMasterTrigger TAME_MASTER = new TameMasterTrigger();
    public static final WaxWaterSensitiveTrigger WAX_WATER_SENSITIVE = new WaxWaterSensitiveTrigger();
    public static final ReleaseControlTrigger RELEASE_CONTROL = new ReleaseControlTrigger();
    public static final FirstCompanionTrigger FIRST_COMPANION = new FirstCompanionTrigger();

    public static void register() {
        CriteriaTriggers.register(CRAFT_CONTROLLER);
        CriteriaTriggers.register(USE_CONTROL_COMMAND);
        CriteriaTriggers.register(TAME_MASTER);
        CriteriaTriggers.register(WAX_WATER_SENSITIVE);
        CriteriaTriggers.register(RELEASE_CONTROL);
        CriteriaTriggers.register(FIRST_COMPANION);
    }
}