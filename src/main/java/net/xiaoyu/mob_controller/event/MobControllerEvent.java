package net.xiaoyu.mob_controller.event;

import net.xiaoyu.mob_controller.util.*;
import net.xiaoyu.mob_controller.capability.*;
import net.xiaoyu.mob_controller.network.ToggleControlModePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.*;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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
    
    // 被控制的生物不破坏方块/包括弹射物
    @SubscribeEvent
    public static void onEntityMobGriefing(@NotNull EntityMobGriefingEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Projectile projectile) {
            entity = projectile.getOwner();
        }
        
        if (entity == null) return;

        if (!(entity instanceof Animal) && entity instanceof Mob mob && MobControlledData.isControlledMob(mob)) {
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
    
    // 被控制的生物离开世界清理
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Mob) {
            Mob mob = (Mob) event.getEntity();

            if (MobControlledData.isControlledMob(mob)) {
                MobControlledData.removeControlledMobOnDeath(mob);
            }
        }
    }
    
    // tick生命值恢复
    @SubscribeEvent
    public static void onLivingTickHeal(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Mob) {
            Mob mob = (Mob) event.getEntity();

            if (MobControlledData.isControlledMob(mob)) {
                mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY).ifPresent(cap -> {
                    long currentTime = mob.level().getGameTime();
                    long lastHealTime = cap.getLastHealTime();
                    boolean hasValidTarget = false;
                    
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
    
    // 被控制的生物受到攻击
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Mob) {
            Mob mob = (Mob) event.getEntity();

            if (MobControlledData.isControlledMob(mob)) {
                if (event.getSource().getEntity() instanceof LivingEntity) {
                    LivingEntity attacker = (LivingEntity) event.getSource().getEntity();
                    UUID controllerUUID = MobControlledData.getControllerUUID(mob);
                    boolean isController = attacker instanceof Player && attacker.getUUID().equals(controllerUUID);

                    // 被控制的生物攻击攻击者[攻击者不是控制者]
                    if (!isController) {
                        if (!MobControlUtil.canControlledMobAttackTarget(mob, attacker)) {
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
    
    // 控制者受到攻击
    @SubscribeEvent
    public static void onControllerAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            if (!player.level().isClientSide() && player.level() instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel) player.level();

                for (Entity entity : serverLevel.getAllEntities()) {
                    if (entity instanceof Mob) {
                        Mob mob = (Mob) entity;

                        if (MobControlledData.isControlledMob(mob)) {
                            UUID controllerUUID = MobControlledData.getControllerUUID(mob);

                            if (controllerUUID != null && controllerUUID.equals(player.getUUID())) {

                                if (event.getSource().getEntity() instanceof LivingEntity) {
                                    LivingEntity attacker = (LivingEntity) event.getSource().getEntity();
                                    
                                    if (mob != attacker && mob.getTarget() == null) {
                                        if (!MobControlUtil.canControlledMobAttackTarget(mob, attacker)) {
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
    
    // 控制者攻击其他生物
    @SubscribeEvent
    public static void onControllerAttackOthers(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player) {
            Player player = (Player) event.getSource().getEntity();

            if (!player.level().isClientSide() && player.level() instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel) player.level();

                for (Entity entity : serverLevel.getAllEntities()) {
                    if (entity instanceof Mob) {
                        Mob mob = (Mob) entity;

                        if (MobControlledData.isControlledMob(mob)) {
                            UUID controllerUUID = MobControlledData.getControllerUUID(mob);
                            
                            if (controllerUUID != null && controllerUUID.equals(player.getUUID())) {
                                if (event.getEntity() instanceof LivingEntity) {
                                    LivingEntity target = (LivingEntity) event.getEntity();
                                    boolean isPlayer = target instanceof Player;
                                    
                                    if (mob != target && mob.getTarget() == null && !isPlayer) {
                                        if (!MobControlUtil.canControlledMobAttackTarget(mob, target)) {
                                            continue;
                                        }
                                        
                                        MobControlledData.markSystemAttack(mob);

                                        // 疣猪兽/僵尸疣猪兽用ATTACK_TARGET内存模块
                                        if (mob instanceof Hoglin/*  || mob instanceof Zoglin */) {
                                            Brain<?> brain = mob.getBrain();
                                            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                                            brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, target, Long.MAX_VALUE);
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
    
    // 被控制的生物攻击的目标是否已死亡[进行清除目标]
    @SubscribeEvent
    public static void onLivingTickCheckTarget(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Mob) {
            Mob mob = (Mob) event.getEntity();
            
            if (MobControlledData.isControlledMob(mob)) {
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
            }
        }
    }

    // 鼠标右键点击切换跟随/停留模式
    @SubscribeEvent
    public static void onPlayerRightClickControlledMob(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == 1) {
            if (Minecraft.getInstance().hitResult instanceof EntityHitResult entityHitResult) {
                if (entityHitResult.getEntity() instanceof Mob mob) {
                    ToggleControlModePacket.INSTANCE.sendToServer(
                        new ToggleControlModePacket(mob.getId())
                    );
                }
            }
        }
    }
}