package net.xiaoyu.mob_controller.event;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.capability.MobControlCapabilityProvider;
import net.xiaoyu.mob_controller.item.AggressiveSwitchItem;
import net.xiaoyu.mob_controller.item.RideCommandItem;
import net.xiaoyu.mob_controller.network.ApplyControlCommandPacket;
import net.xiaoyu.mob_controller.network.MobControlCapabilitySyncPacket;
import net.xiaoyu.mob_controller.network.NetWorkManager;
import net.xiaoyu.mob_controller.network.SwitchAggressiveModePacket;
import net.xiaoyu.mob_controller.registry.ModItems;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import net.minecraft.world.phys.AABB;
import java.util.List;
import java.util.Comparator;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * 生物控制系统事件处理器。
 *
 * <p>集中处理能力附加、攻击联动、模式指令、目标同步与重生调度等 Forge 事件。</p>
 */
@Mod.EventBusSubscriber
public class MobControllerEvent {
    private static final int HEAL_INTERVAL_TICKS = 2;

    /**
     * 为生物实体附加控制能力。
     */
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
     * 拦截受控生物（含其弹射物）的方块破坏行为。
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

        if (!(entity instanceof Animal) && !(entity instanceof Piglin) && entity instanceof Mob mob && MobControlledData.isControlledEntity(
                mob)) {
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
     * 生物离开世界时清理受控记录。
     */
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Mob mob) {
            // 仅在实体真正死亡离场时移除高生命限制记录，避免跨维度/卸载导致限制失效。
            if (MobControlledData.isControlledEntity(mob) && !mob.isAlive() && mob.getHealth() <= 0.0F) {
                MobControlledData.removeControlledMobOnDeath(mob);
            }
        }
    }

    /**
     * 受控生物死亡时安排延迟重生。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        if (event.getEntity() instanceof Mob mob && mob.level() instanceof ServerLevel serverLevel
                && MobControlledData.isControlledEntity(mob)) {

            if (!Config.ENABLE_RESPAWN.get()) {
                if (mob.level() instanceof ServerLevel deathLevel) {
                    Player controller = MobControlledData.getController(mob, deathLevel);
                    if (controller instanceof ServerPlayer serverPlayer) {
                        String deathCause = extractDeathCause(event.getSource(), mob);
                        serverPlayer.sendSystemMessage(Component.translatable("mob_controller.message.death_cause",
                                mob.getDisplayName(), deathCause));
                    }
                }
                return;
            }

            String deathCause = extractDeathCause(event.getSource(), mob);

            if (MobControlledData.scheduleRespawn(mob, serverLevel, deathCause)) {
                Player controller = MobControlledData.getController(mob, serverLevel);
                if (controller instanceof ServerPlayer serverPlayer) {
                    int seconds = Config.RESPAWN_DELAY_TICKS.get() / 20;
                    // 使用新的本地化键，三个参数：生物名、死因、秒数
                    serverPlayer.sendSystemMessage(Component.translatable(
                            "mob_controller.message.respawn_scheduled_cause",
                            mob.getDisplayName(),
                            deathCause,
                            seconds
                    ));
                }
            }
        }
    }

    // 辅助方法：从 DamageSource 提取纯粹的死因短语（去掉实体名称）
    private static String extractDeathCause(DamageSource source, LivingEntity victim) {
        Component deathMessage = source.getLocalizedDeathMessage(victim);
        String fullMessage = deathMessage.getString();

        // 移除生物名称前缀（假设格式为 "生物名 死因"）
        String victimName = victim.getName().getString();
        if (fullMessage.startsWith(victimName)) {
            String suffix = fullMessage.substring(victimName.length()).trim();
            if (!suffix.isEmpty()) return suffix;
        }
        // 保底：使用死亡消息的原始文本（可能含生物名）
        return fullMessage;
    }

    /**
     * 服务端每刻处理待重生队列。
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MobControlledData.tickPendingRespawns(event.getServer());
        }
    }

    /**
     * 受控生物每刻自动恢复生命值（无有效目标时）。
     */
    @SubscribeEvent
    public static void onLivingTickHeal(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Mob mob) {
            if (mob.level().isClientSide) {
                return;
            }
            if (!MobControlledData.isControlledEntity(mob)) return;
            if (MobControlledData.isSummoned(mob)) return;   // 新增：召唤物不自动回血
            if (MobControlledData.isControlledEntity(mob)) {
                mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY).ifPresent(cap -> {
                    long currentTime = mob.level().getGameTime();
                    long lastHealTime = cap.getLastHealTime();
                    boolean inCombat = hasActiveCombatActivity(mob);

                    if (inCombat) {
                        cap.setLastCombatTime(currentTime);
                    }

                    // 每2tick恢复1生命值[没有有效攻击目标且已脱战]
                    if (currentTime - lastHealTime >= HEAL_INTERVAL_TICKS
                            && !inCombat
                            && currentTime - cap.getLastCombatTime() >= Config.CONTROLLED_MOB_HEAL_OUT_OF_COMBAT_DELAY_TICKS.get()) {
                        if (mob.getHealth() < mob.getMaxHealth()) {
                            mob.heal(1.0F);
                            cap.setLastHealTime(currentTime);
                        }
                    }
                });
            }
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof RideCommandItem && !player.level().isClientSide) {
            if (RideCommandItem.handleLeftClick((ServerPlayer) player, target, mainHand)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onAttackEntityWithAggressiveSwitch(AttackEntityEvent event) {
        Player player = event.getEntity();
        ItemStack mainHand = player.getMainHandItem();

        if (!(mainHand.getItem() instanceof AggressiveSwitchItem)) {
            return;
        }

        Entity target = event.getTarget();
        if (!(target instanceof Mob mob)) {
            return;
        }

        // 潜行时不处理单体（由鼠标事件处理批量）
        if (player.isShiftKeyDown()) {
            return;
        }

        // 检查是否为玩家控制的生物
        if (!MobControlledData.isControlledEntity(mob) ||
                !Objects.equals(MobControlledData.getControllerUUID(mob), player.getUUID())) {
            return;
        }

        // 取消攻击伤害
        event.setCanceled(true);

        // 发送单体切换包（索敌模式 = true）
        if (!player.level().isClientSide) {
            NetWorkManager.INSTANCE.sendToServer(new SwitchAggressiveModePacket(true, mob.getId()));
        } else {
            // 客户端播放手臂摆动动画
            player.swing(InteractionHand.MAIN_HAND);
        }
    }

    /**
     * 受控生物受攻击时触发反击目标设置。
     */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Mob mob) {

            if (MobControlledData.isControlledEntity(mob)) {
                LivingEntity attacker = getResponsibleLivingEntity(event.getSource().getEntity());
                if (attacker != null) {
                    UUID controllerUUID = MobControlledData.getControllerUUID(mob);
                    boolean isController = attacker instanceof Player && attacker.getUUID().equals(controllerUUID);

                    // 被控制的生物攻击攻击者[攻击者不是控制者]
                    if (!isController) {
                        if (!MobControlUtil.canRetaliateAgainstImmediateAttacker(mob, attacker)) {
                            return;
                        }

                        mob.setLastHurtByMob(attacker);

                        MobControlledData.markCombat(mob);

                        MobControlledData.markSystemAttack(mob);

                        // 疣猪兽/僵尸疣猪兽用ATTACK_TARGET内存模块
                        if (mob instanceof Hoglin || mob instanceof Zoglin) {
                            Brain<?> brain = mob.getBrain();
                            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                            brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, attacker, Long.MAX_VALUE);
                        } else if (mob instanceof Piglin || mob instanceof PiglinBrute) {
                            // 猪灵/猪灵蛮兵用ANGRY_AT内存模块
                            Brain<?> brain = mob.getBrain();
                            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                            brain.setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, attacker.getUUID(), 600L);
                            brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, attacker, 200L);
                        } else {
                            MobControlUtil.setMobTargetWithAnger(mob, attacker);
                        }
                    }
                }
            }
        }
    }

    /**
     * 控制者受攻击时，调度其受控生物进行援护反击。
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

                                LivingEntity attacker = getResponsibleLivingEntity(event.getSource().getEntity());
                                if (attacker != null) {

                                    boolean attackerIsOtherPlayer = attacker instanceof Player attackerPlayer
                                            && !attackerPlayer.getUUID().equals(controllerUUID);

                                    // 其他玩家攻击主人时，允许优先切换为护主目标。
                                    if (!mob.equals(attacker) && (mob.getTarget() == null || attackerIsOtherPlayer)) {
                                        if (!MobControlUtil.canRetaliateAgainstImmediateAttacker(mob, attacker)) {
                                            continue;
                                        }

                                        player.setLastHurtByMob(attacker);

                                        MobControlledData.markCombat(mob);
                                        MobControlledData.markSystemAttack(mob);

                                        // 疣猪兽/僵尸疣猪兽用ATTACK_TARGET内存模块
                                        if (mob instanceof Hoglin || mob instanceof Zoglin) {
                                            Brain<?> brain = mob.getBrain();
                                            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                                            brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, attacker, Long.MAX_VALUE);
                                        } else if (mob instanceof Piglin || mob instanceof PiglinBrute) {
                                            // 猪灵/猪灵蛮兵用ANGRY_AT内存模块
                                            Brain<?> brain = mob.getBrain();
                                            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                                            brain.setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, attacker.getUUID(), 600L);
                                            brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, attacker, 200L);
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
     * 每刻处理受控生物的索敌模式：主动寻找并锁定敌对生物，但不覆盖已有的有效目标。
     * 对猪灵、疣猪兽等基于 Brain 的生物使用记忆模块设置目标。
     * 加入冷却机制避免频繁操作导致AI抽搐。
     */
    @SubscribeEvent
    public static void onAggressiveModeTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (mob.level().isClientSide) {
            return;
        }
        if (!MobControlledData.isControlledEntity(mob)) {
            return;
        }
        // 只处理索敌模式
        if (!MobControlledData.isAggressiveMode(mob)) {
            return;
        }

        // 冷却：每 20 tick（1秒）扫描一次，避免过度操作
        if (mob.tickCount % 20 != 0) {
            return;
        }

        // 获取当前目标
        LivingEntity currentTarget = mob.getTarget();

        // 判断当前目标是否有效（存活、可攻击、且仍为敌对）
        boolean hasValidTarget = false;
        if (currentTarget != null && currentTarget.isAlive() && !currentTarget.isDeadOrDying()) {
            if (MobControlUtil.canKeepCombatTarget(mob, currentTarget)) {
                hasValidTarget = true;
            } else {
                // 当前目标不再敌对，清除记忆
                mob.setTarget(null);
                if (mob instanceof AbstractPiglin || mob instanceof Hoglin || mob instanceof Zoglin) {
                    Brain<?> brain = mob.getBrain();
                    brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
                    brain.eraseMemory(MemoryModuleType.ANGRY_AT);
                }
            }
        }

        // 如果有有效目标则跳过搜索
        if (hasValidTarget) {
            return;
        }

        // 判断是否为基于 Brain 的生物
        boolean isBrainMob = mob instanceof AbstractPiglin || mob instanceof Hoglin || mob instanceof Zoglin;

        // 搜寻攻击范围内的敌对生物
        double followRange = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        AABB searchArea = mob.getBoundingBox().inflate(followRange, 4.0, followRange);
        List<LivingEntity> potentialTargets = mob.level().getEntitiesOfClass(
                LivingEntity.class, searchArea,
                target -> target.isAlive() && !target.isDeadOrDying() && MobControlUtil.isHostileTarget(mob, target)
        );

        if (!potentialTargets.isEmpty()) {
            potentialTargets.sort(Comparator.comparingDouble(mob::distanceToSqr));
            LivingEntity bestTarget = potentialTargets.get(0);

            MobControlledData.markSystemAttack(mob);

            if (isBrainMob) {
                Brain<?> brain = mob.getBrain();
                // 检查记忆中的 ATTACK_TARGET 是否已经是这个目标
                boolean needSet = true;
                var existingTarget = brain.getMemory(MemoryModuleType.ATTACK_TARGET);
                if (existingTarget.isPresent() && existingTarget.get() == bestTarget) {
                    needSet = false;
                }

                if (needSet) {
                    brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                    brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, bestTarget, 200L);
                    if (mob instanceof AbstractPiglin) {
                        brain.setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, bestTarget.getUUID(), 600L);
                    }
                }
                // 同时设置传统目标以辅助
                mob.setTarget(bestTarget);
            } else {
                MobControlUtil.setMobTargetWithAnger(mob, bestTarget);
            }
        }
    }

    /**
     * 控制者攻击其他生物时，调度受控生物协同攻击。
     * 修改：现在即使受控生物已有战斗目标，也会强制切换到玩家攻击的目标（玩家指令优先）。
     */
    @SubscribeEvent
    public static void onControllerAttackOthers(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {

            if (!player.level().isClientSide() && player.level() instanceof ServerLevel serverLevel) {
                LivingEntity playerTarget = event.getEntity(); // 玩家攻击的目标

                for (Entity entity : serverLevel.getAllEntities()) {
                    if (entity instanceof Mob mob) {
                        if (MobControlledData.isControlledEntity(mob)) {
                            UUID controllerUUID = MobControlledData.getControllerUUID(mob);
                            if (controllerUUID != null && controllerUUID.equals(player.getUUID())) {
                                // 玩家指挥攻击：强制覆盖现有目标，不再检查 canKeepCombatTarget
                                // 但需要检查新目标是否允许攻击（如是否为控制者本人，是否配置允许攻击玩家等）
                                boolean canAttackTarget = playerTarget instanceof Player
                                        ? MobControlUtil.canAttackPlayerByOwnerCommand(mob, playerTarget)
                                        : MobControlUtil.isEnemy(mob, playerTarget);
                                if (!canAttackTarget) {
                                    continue;
                                }

                                // 清除当前仇恨
                                mob.setTarget(null);
                                if (mob instanceof AbstractPiglin || mob instanceof Hoglin || mob instanceof Zoglin) {
                                    Brain<?> brain = mob.getBrain();
                                    brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
                                    brain.eraseMemory(MemoryModuleType.ANGRY_AT);
                                }

                                MobControlledData.markCombat(mob);
                                MobControlledData.markSystemAttack(mob);

                                // 设置新目标
                                if (mob instanceof Hoglin || mob instanceof Zoglin) {
                                    Brain<?> brain = mob.getBrain();
                                    brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                                    brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, playerTarget, 200L);
                                } else if (mob instanceof Piglin || mob instanceof PiglinBrute) {
                                    Brain<?> brain = mob.getBrain();
                                    brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                                    brain.setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, playerTarget.getUUID(), 600L);
                                    brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, playerTarget, 200L);
                                } else {
                                    MobControlUtil.setMobTargetWithAnger(mob, playerTarget);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 受控生物造成伤害时刷新战斗时间，兼容箭矢/药水等投射物来源。
     */
    @SubscribeEvent
    public static void onControlledMobDealDamage(LivingHurtEvent event) {
        Mob sourceMob = getResponsibleMob(event.getSource().getEntity());
        if (sourceMob != null && MobControlledData.isControlledEntity(sourceMob)) {
            MobControlledData.markCombat(sourceMob);
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

                    // Brain 类生物可能仅通过 ATTACK_TARGET 维持战斗，不应因 setTarget 为空而提前脱战。
                    if (MobControlledData.isSystemAttack(mob) && !hasValidCombatTarget(mob)) {
                        MobControlledData.clearSystemAttack(mob);
                    }
                }

                if (!mob.level().isClientSide) {
                    mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY).ifPresent(cap ->
                            NetWorkManager.INSTANCE.send(
                                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(event::getEntity),
                                    new MobControlCapabilitySyncPacket(mob.getId(), cap.serializeNBT())
                            ));
                }
            }
        }
    }

    // 修改原有的 onPlayerRightClickControlledMob 方法
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onPlayerRightClickControlledMob(InputEvent.MouseButton.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.screen != null || event.getAction() != InputConstants.RELEASE) {
            return;
        }

        ItemStack mainHand = mc.player.getMainHandItem();

        // 控制令逻辑（保持不变）
        if (mainHand.is(ModItems.CONTROL_COMMAND_ITEM.get())) {
            MobControlledData.ControlMode mode = switch (event.getButton()) {
                case InputConstants.MOUSE_BUTTON_LEFT -> MobControlledData.ControlMode.FOLLOW;
                case InputConstants.MOUSE_BUTTON_RIGHT -> MobControlledData.ControlMode.STAY;
                case InputConstants.MOUSE_BUTTON_MIDDLE -> MobControlledData.ControlMode.WANDER;
                default -> null;
            };
            if (mode != null) {
                NetWorkManager.INSTANCE.sendToServer(new ApplyControlCommandPacket(mode));
                // 播放使用动画
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        }
        // 护主切换器逻辑（修改：支持批量切换）
        else if (mainHand.is(ModItems.AGGRESSIVE_SWITCH_ITEM.get())) {
            int button = event.getButton();
            if (button != InputConstants.MOUSE_BUTTON_LEFT && button != InputConstants.MOUSE_BUTTON_RIGHT) {
                return;
            }
            boolean isSneaking = mc.player.isShiftKeyDown();
            boolean aggressive = button == InputConstants.MOUSE_BUTTON_LEFT; // 左键为索敌模式，右键为护主模式

            // 播放使用动画
            mc.player.swing(InteractionHand.MAIN_HAND);

            if (isSneaking) {
                // 潜行模式：批量切换
                NetWorkManager.INSTANCE.sendToServer(new SwitchAggressiveModePacket(aggressive, -1));
            } else {
                // 非潜行模式：瞄准单体切换
                Entity targetedEntity = mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY
                        ? ((EntityHitResult) mc.hitResult).getEntity() : null;
                if (targetedEntity instanceof Mob mob &&
                        MobControlledData.isControlledEntity(mob) &&
                        Objects.equals(MobControlledData.getControllerUUID(mob), mc.player.getUUID())) {
                    NetWorkManager.INSTANCE.sendToServer(new SwitchAggressiveModePacket(aggressive, mob.getId()));
                } else {
                    // 未瞄准有效受控生物，发送无效单体包（服务端会反馈消息）
                    NetWorkManager.INSTANCE.sendToServer(new SwitchAggressiveModePacket(aggressive, -2));
                }
            }
        }
    }

    /**
     * 玩家与可骑乘受控生物交互时，允许控制者直接骑乘。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack mainHandItem = event.getEntity().getMainHandItem();
        if (
                !(event.getTarget() instanceof Mob mob)
                        || mainHandItem.is(ModItems.MOB_CONTROLLER_ITEM.get())
                        || mainHandItem.is(ModItems.HEART_CONTRACT_ITEM.get())
                        || event.getEntity().isShiftKeyDown()
        ) {
            return;
        }
        if (!mainHandItem.isEmpty() || !MobControlUtil.isDirectRideableControlledMob(mob)) {
            return;
        }
        if (
                !MobControlledData.isControlledEntity(mob)
                        || !Objects.equals(
                        MobControlledData.getControllerUUID(mob),
                        event.getEntity().getUUID()
                )
        ) {
            return;
        }
        event.getEntity().startRiding(event.getTarget());
        event.setResult(Event.Result.ALLOW);
        event.setCanceled(true);
    }

    /**
     * 目标切换事件中过滤受控生物对非敌对目标的锁定。
     */
    @SubscribeEvent
    public static void onLivingChangeTargetEvent(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob mob && event.getNewTarget() != null) {
            if (MobControlledData.isControlledEntity(mob)
                    && !MobControlUtil.canKeepCombatTarget(mob, event.getNewTarget())) {
            } else if (MobControlledData.isControlledEntity(mob) && isValidCombatTarget(mob, event.getNewTarget())) {
                MobControlledData.markCombat(mob);
            }
        }
    }

    private static boolean hasValidCombatTarget(Mob mob) {
        if (mob instanceof Hoglin || mob instanceof Zoglin || mob instanceof AbstractPiglin) {
            Brain<?> brain = mob.getBrain();
            Optional<LivingEntity> attackTarget = brain.getMemory(MemoryModuleType.ATTACK_TARGET);
            if (attackTarget.isPresent() && isValidCombatTarget(mob, attackTarget.get())) {
                return true;
            }
        }

        return isValidCombatTarget(mob, mob.getTarget());
    }

    private static boolean hasActiveCombatActivity(Mob mob) {
        if (hasValidCombatTarget(mob)) {
            return true;
        }
        LivingEntity lastHurtBy = mob.getLastHurtByMob();
        if (isValidCombatTarget(mob, lastHurtBy)) {
            if (mob.tickCount - mob.getLastHurtByMobTimestamp() <= 10) {
                return true;
            }
        }
        LivingEntity lastHurt = mob.getLastHurtMob();
        if (isValidCombatTarget(mob, lastHurt)) {
            if (mob.tickCount - mob.getLastHurtMobTimestamp() <= 10) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidCombatTarget(Mob mob, @Nullable LivingEntity target) {
        return target != null
                && target.isAlive()
                && !target.isDeadOrDying()
                && target.level().equals(mob.level())
                && target.distanceToSqr(mob) <= 64.0D * 64.0D;
    }

    @Nullable
    private static LivingEntity getResponsibleLivingEntity(@Nullable Entity sourceEntity) {
        if (sourceEntity instanceof LivingEntity livingEntity) {
            return livingEntity;
        }
        if (sourceEntity instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity livingEntity) {
            return livingEntity;
        }
        return null;
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Pillager pillager && !event.getEntity().level().isClientSide) {
            if (MobControlledData.isControlledEntity(pillager)) {
                ItemStack mainHand = pillager.getMainHandItem();
                if (mainHand.is(Items.CROSSBOW) && mainHand.isDamaged()) {
                    mainHand.setDamageValue(0);
                }
            }
        }
    }

    @Nullable
    private static Mob getResponsibleMob(@Nullable Entity sourceEntity) {
        LivingEntity livingEntity = getResponsibleLivingEntity(sourceEntity);
        if (livingEntity instanceof Mob mob) {
            return mob;
        }
        return null;
    }
}