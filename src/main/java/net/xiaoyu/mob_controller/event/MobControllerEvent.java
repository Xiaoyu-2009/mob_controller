package net.xiaoyu.mob_controller.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xiaoyu.mob_controller.util.*;
import net.xiaoyu.mob_controller.capability.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.LazyOptional;

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
    
    // 被控制的生物你不能破坏方块/包括弹射物
    @SubscribeEvent
    public static void onMobGriefingCheck(EntityMobGriefingEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Mob) {
            Mob mob = (Mob) entity;

            if (MobControlledData.isControlledMob(mob)) {
                event.setResult(EntityMobGriefingEvent.Result.DENY);
            }
        } else if (entity instanceof Projectile) {
            Projectile projectile = (Projectile) entity;
            Entity owner = projectile.getOwner();
            
            if (owner instanceof Mob && MobControlledData.isControlledMob((Mob) owner)) {
                event.setResult(EntityMobGriefingEvent.Result.DENY);
            }
        }
    }

    // 其他生物对被控制的生物中立
    @SubscribeEvent
    public static void onLivingChangeTargetNeutral(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof Mob) {
            Mob target = (Mob) event.getNewTarget();

            if (MobControlledData.isControlledMob(target)) {
                event.setCanceled(true);
            }
        }
    }
    
    // 被控制的生物对其他生物中立
    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob) {
            Mob mob = (Mob) event.getEntity();
            if (MobControlledData.isControlledMob(mob)) {
                if (event.getNewTarget() != null) {
                    if (!MobControlUtil.canControlledMobAttackTarget(mob, event.getNewTarget())) {
                        event.setCanceled(true);
                        return;
                    }
                }

                MobControlledData.clearSystemAttack(mob);
            }
        }
    }
    
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
                LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
                capability.ifPresent(cap -> {
                    long currentTime = mob.level().getGameTime();
                    long lastHealTime = cap.getLastHealTime();
                    
                    // 每2tick恢复1生命值[没有攻击目标]
                    if (currentTime - lastHealTime >= 2 && mob.getTarget() == null) {
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
                        MobControlUtil.setMobTargetWithAnger(mob, attacker);
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