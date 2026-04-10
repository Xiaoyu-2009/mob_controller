package net.xiaoyu.mob_controller.event;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.xiaoyu.mob_controller.capability.MobControlCapabilityProvider;
import net.xiaoyu.mob_controller.entity.EntityControlledWitch;
import net.xiaoyu.mob_controller.network.ApplyControlCommandPacket;
import net.xiaoyu.mob_controller.network.MobControlCapabilitySyncPacket;
import net.xiaoyu.mob_controller.network.NetWorkManager;
import net.xiaoyu.mob_controller.registry.ModItems;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber
public class MobControllerEvent {

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Mob) {
            event.addCapability(
                    new ResourceLocation("mob_controller", "mob_control"),
                    new MobControlCapabilityProvider()
            );
        }
    }

    /**
     * 被控制的生物不破坏方块/包括弹射物
     */
    @SubscribeEvent
    public static void onEntityMobGriefing(EntityMobGriefingEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Projectile projectile) {
            entity = projectile.getOwner();
        }

        if (entity == null) {
            return;
        }

        if (!(entity instanceof Animal) && !(entity instanceof Piglin) && entity instanceof Mob mob && MobControlledData.isControlledEntity(mob)) {
            event.setResult(Event.Result.DENY);
        }
    }

    // 被控制的生物/其他生物中立
    /*@SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob) {
            Mob mob = (Mob) event.getEntity();
            if (MobControlledData.isControlledMob(mob)) {
                if (!MobControlledData.isSystemAttack(mob)) {
                    event.setCanceled(true);
                } else {
                    MobControlledData.clearSystemAttack(mob);
                }
            } else if (MobControlledData.isControlledEntity(event.getNewTarget())) {
                event.setCanceled(true);
            }
        }
    }*/

    /**
     * 被控制的生物离开世界清理
     */
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Mob mob) {

            if (MobControlledData.isControlledEntity(mob)) {
                MobControlledData.removeControlledMobOnDeath(mob);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Mob mob && mob.level() instanceof ServerLevel serverLevel
                && MobControlledData.isControlledEntity(mob)) {
            if (MobControlledData.scheduleRespawn(mob, serverLevel)) {
                MinecraftServer server = serverLevel.getServer();
                String message = mob.getDisplayName().getString() + "死了，将在30秒后复活";
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "say " + message);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MobControlledData.tickPendingRespawns(event.getServer());
        }
    }

    /**
     * tick生命值恢复
     */
    @SubscribeEvent
    public static void onLivingTickHeal(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Mob mob) {

            if (MobControlledData.isControlledEntity(mob)) {
                mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY).ifPresent(cap -> {
                    long currentTime = mob.level().getGameTime();
                    long lastHealTime = cap.getLastHealTime();
                    boolean hasValidTarget;

                    // 疣猪兽/僵尸疣猪兽用ATTACK_TARGET内存模块
                    if (mob instanceof Hoglin/*  || mob instanceof Zoglin */) {
                        Brain<?> brain = mob.getBrain();
                        Optional<LivingEntity> attackTarget = brain.getMemory(MemoryModuleType.ATTACK_TARGET);
                        hasValidTarget = attackTarget.isPresent() && attackTarget.get().isAlive() && !attackTarget.get().isDeadOrDying();
                    }
                    // 猪灵/猪灵蛮兵用ANGRY_AT和ATTACK_TARGET内存模块
                    else if (mob instanceof AbstractPiglin) {
                        Brain<?> brain = mob.getBrain();
                        Optional<LivingEntity> attackTarget = brain.getMemory(MemoryModuleType.ATTACK_TARGET);
                        hasValidTarget = brain.getMemory(MemoryModuleType.ANGRY_AT).isPresent() && attackTarget.isPresent() && attackTarget.get().isAlive() && !attackTarget.get().isDeadOrDying();
                    } else {
                        LivingEntity target = mob.getTarget();
                        hasValidTarget = target != null && target.isAlive() && !target.isDeadOrDying();
                    }

                    // 每2tick恢复1生命值[没有有效攻击目标]
                    if (currentTime - lastHealTime >= 2 && !hasValidTarget) {
                        if (mob.getHealth() < mob.getMaxHealth()) {
                            mob.heal(1.0F);
                            cap.setLastHealTime(currentTime);
                        }
                    }
                });
            }
        }
    }

    /**
     * 被控制的生物受到攻击
     */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Mob mob) {

            if (MobControlledData.isControlledEntity(mob)) {
                if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                    UUID controllerUUID = MobControlledData.getControllerUUID(mob);
                    boolean isController = attacker instanceof Player && attacker.getUUID().equals(controllerUUID);

                    // 被控制的生物攻击攻击者[攻击者不是控制者]
                    if (!isController) {
                        if (!MobControlUtil.isEnemy(mob, attacker)) {
                            return;
                        }

                        MobControlledData.markSystemAttack(mob);

                        // 疣猪兽/僵尸疣猪兽用ATTACK_TARGET内存模块
                        if (mob instanceof Hoglin/*  || mob instanceof Zoglin */) {
                            Brain<?> brain = mob.getBrain();
                            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                            brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, attacker, Long.MAX_VALUE);
                        } else if (mob instanceof Piglin || mob instanceof PiglinBrute) {
                            // 猪灵/猪灵蛮兵用ANGRY_AT内存模块
                            Brain<?> brain = mob.getBrain();
                            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                            brain.setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, attacker.getUUID(), 600L);
                        } else {
                            MobControlUtil.setMobTargetWithAnger(mob, attacker);
                        }
                    }
                }
            }
        }
    }

    /**
     * 控制者受到攻击
     */
    @SubscribeEvent
    public static void onControllerAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player player) {

            if (!player.level().isClientSide() && player.level() instanceof ServerLevel serverLevel) {

                for (Entity entity : serverLevel.getAllEntities()) {
                    if (entity instanceof Mob mob) {

                        if (MobControlledData.isControlledEntity(mob)) {
                            UUID controllerUUID = MobControlledData.getControllerUUID(mob);

                            if (controllerUUID != null && controllerUUID.equals(player.getUUID())) {

                                if (event.getSource().getEntity() instanceof LivingEntity attacker) {

                                    if (!mob.equals(attacker) && mob.getTarget() == null) {
                                        if (!MobControlUtil.isEnemy(mob, attacker)) {
                                            continue;
                                        }

                                        MobControlledData.markSystemAttack(mob);

                                        // 疣猪兽/僵尸疣猪兽用ATTACK_TARGET内存模块
                                        if (mob instanceof Hoglin/*  || mob instanceof Zoglin */) {
                                            Brain<?> brain = mob.getBrain();
                                            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                                            brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, attacker, Long.MAX_VALUE);
                                        } else if (mob instanceof Piglin || mob instanceof PiglinBrute) {
                                            // 猪灵/猪灵蛮兵用ANGRY_AT内存模块
                                            Brain<?> brain = mob.getBrain();
                                            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                                            brain.setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, attacker.getUUID(), 600L);
                                        } else {
                                            MobControlUtil.setMobTargetWithAnger(mob, attacker);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 控制者攻击其他生物
     */
    @SubscribeEvent
    public static void onControllerAttackOthers(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {

            if (!player.level().isClientSide() && player.level() instanceof ServerLevel serverLevel) {

                for (Entity entity : serverLevel.getAllEntities()) {
                    if (entity instanceof Mob mob) {

                        if (MobControlledData.isControlledEntity(mob)) {
                            UUID controllerUUID = MobControlledData.getControllerUUID(mob);

                            if (controllerUUID != null && controllerUUID.equals(player.getUUID())) {
                                if (event.getEntity() instanceof LivingEntity) {
                                    LivingEntity target = event.getEntity();
                                    boolean isPlayer = target instanceof Player;

                                    if (!mob.equals(target) && mob.getTarget() == null && !isPlayer) {
                                        if (!MobControlUtil.isEnemy(mob, target)) {
                                            continue;
                                        }

                                        MobControlledData.markSystemAttack(mob);

                                        // 疣猪兽/僵尸疣猪兽用ATTACK_TARGET内存模块
                                        if (mob instanceof Hoglin || mob instanceof Zoglin) {
                                            Brain<?> brain = mob.getBrain();
                                            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                                            brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, target, 200L);
                                        } else if (mob instanceof Piglin || mob instanceof PiglinBrute) {
                                            // 猪灵/猪灵蛮兵用ANGRY_AT内存模块
                                            Brain<?> brain = mob.getBrain();
                                            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                                            brain.setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, target.getUUID(), 600L);
                                        } else {
                                            MobControlUtil.setMobTargetWithAnger(mob, target);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 被控制的生物攻击的目标是否已死亡[进行清除目标]
     */
    @SubscribeEvent
    public static void onLivingTickCheckTarget(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Mob mob) {

            if (MobControlledData.isControlledEntity(mob)) {
                LivingEntity target = mob.getTarget();

                // 目标不存在/死亡/不再存活时清除
                if (target == null || target.isDeadOrDying() || !target.isAlive() ||
                        !target.level().equals(mob.level()) || target.distanceTo(mob) > 64.0F) {

                    if (target != null) {
                        mob.setTarget(null);
                    }

                    if (MobControlledData.isSystemAttack(mob)) {
                        MobControlledData.clearSystemAttack(mob);
                    }
                }

                if (!mob.level().isClientSide) {
                    mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY).ifPresent(cap ->
                            NetWorkManager.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(event::getEntity),
                                    new MobControlCapabilitySyncPacket(mob.getId(), cap.serializeNBT())));
                }
            }
        }
    }

    /**
     * 手持控制器时，左/右/中键对32格内已控制生物下达模式命令
     */
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onPlayerRightClickControlledMob(InputEvent.MouseButton.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.screen != null || !mc.player.getMainHandItem().is(ModItems.CONTROL_COMMAND_ITEM.get())) {
            return;
        }

        if (event.getAction() != InputConstants.RELEASE) {
            return;
        }

        MobControlledData.ControlMode mode = switch (event.getButton()) {
            case InputConstants.MOUSE_BUTTON_LEFT -> MobControlledData.ControlMode.FOLLOW;
            case InputConstants.MOUSE_BUTTON_RIGHT -> MobControlledData.ControlMode.STAY;
            case InputConstants.MOUSE_BUTTON_MIDDLE -> MobControlledData.ControlMode.WANDER;
            default -> null;
        };

        if (mode != null) {
            NetWorkManager.INSTANCE.sendToServer(new ApplyControlCommandPacket(mode));
        }
    }

    @SubscribeEvent
    public static void onPlayerEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof Mob mob
                && !event.getEntity().getMainHandItem().is(ModItems.MOB_CONTROLLER_ITEM.get())
                && !event.getEntity().getMainHandItem().is(ModItems.HEART_CONTRACT_ITEM.get())) {
            if (mob instanceof Guardian ||
                    mob instanceof Hoglin ||
                    mob instanceof Zoglin ||
                    mob instanceof Ravager ||
                    mob instanceof Cow ||
                    mob instanceof Sheep ||
                    mob instanceof Dolphin) {
                if (MobControlledData.isControlledEntity(mob) && Objects.equals(MobControlledData.getControllerUUID(mob), event.getEntity().getUUID())) {
                    event.getEntity().startRiding(event.getTarget());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTargetEvent(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob mob && event.getNewTarget() != null) {
            if (MobControlledData.isControlledEntity(mob) && !MobControlUtil.isEnemy(mob, event.getNewTarget())) {
                if (!(mob instanceof EntityControlledWitch)) {
                    event.setCanceled(true);
                }
            }
        }
    }
}