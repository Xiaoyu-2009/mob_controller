package net.xiaoyu.mob_controller.event;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.xiaoyu.mob_controller.ClientConfig;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.MobController;
import net.xiaoyu.mob_controller.advancement.MobControllerTriggers;
import net.xiaoyu.mob_controller.capability.MobControlCapabilityProvider;
import net.xiaoyu.mob_controller.config.FeatureConfig;
import net.xiaoyu.mob_controller.item.AggressiveSwitchItem;
import net.xiaoyu.mob_controller.item.LegionBannerItem;
import net.xiaoyu.mob_controller.item.ModeSelectControlCommandItem;
import net.xiaoyu.mob_controller.item.RideCommandItem;
import net.xiaoyu.mob_controller.network.*;
import net.xiaoyu.mob_controller.registry.ModItems;
import net.xiaoyu.mob_controller.registry.ModSounds;
import net.xiaoyu.mob_controller.util.HighHealthDatabase;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;
import net.minecraft.nbt.CompoundTag;
import net.xiaoyu.mob_controller.network.SyncLegionColorPacket;
import net.xiaoyu.mob_controller.network.SyncLegionModePacket;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 生物控制系统事件处理器。
 *
 * <p>集中处理能力附加、攻击联动、模式指令、目标同步与重生调度等 Forge 事件。</p>
 */
@Mod.EventBusSubscriber
public class MobControllerEvent {
    private static final int HEAL_INTERVAL_TICKS = 2;

    /**
     * 玩家重生（从死亡状态复活）时，将军团模式状态和队伍颜色从旧玩家复制到新玩家。
     * 因为 PersistentData 不会自动保留，需要在 Clone 事件中手动迁移。
     *
     * @param event PlayerEvent.Clone 事件
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player player = event.getEntity();
        if (event.isWasDeath() && original != null && player != null) {
            CompoundTag oldData = original.getPersistentData();
            CompoundTag newData = player.getPersistentData();

            // 需要复制的键列表
            String[] keysToCopy = {
                    "mob_controller_tamed_types",
                    "mob_controller_had_high_health",
                    "mob_controller_first_tame",
                    "LegionColor",
                    "mob_controller_player_legion_mode"
            };
            for (String key : keysToCopy) {
                if (oldData.contains(key)) {
                    // 简单处理基本类型
                    net.minecraft.nbt.Tag tag = oldData.get(key);
                    if (tag instanceof net.minecraft.nbt.StringTag) {
                        newData.putString(key, oldData.getString(key));
                    } else if (tag instanceof net.minecraft.nbt.ByteTag) {
                        newData.putBoolean(key, oldData.getBoolean(key));
                    } else if (tag instanceof net.minecraft.nbt.IntTag) {
                        newData.putInt(key, oldData.getInt(key));
                    } else if (tag instanceof net.minecraft.nbt.LongTag) {
                        newData.putLong(key, oldData.getLong(key));
                    } else if (tag instanceof net.minecraft.nbt.DoubleTag) {
                        newData.putDouble(key, oldData.getDouble(key));
                    } else if (tag instanceof net.minecraft.nbt.CompoundTag) {
                        newData.put(key, tag.copy());
                    } else if (tag instanceof net.minecraft.nbt.ListTag) {
                        newData.put(key, tag.copy());
                    }
                }
            }

            // 如果是服务端玩家，将军团模式和队伍颜色重新同步给所有客户端
            if (player instanceof ServerPlayer serverPlayer) {
                boolean legionMode = LegionBannerItem.isPlayerInLegionMode(serverPlayer);
                NetWorkManager.INSTANCE.send(PacketDistributor.ALL.noArg(),
                        new SyncLegionModePacket(serverPlayer.getUUID(), legionMode));
                ChatFormatting color = LegionBannerItem.getLegionColor(serverPlayer);
                int rgb = LegionBannerItem.getColorRGB(color);
                NetWorkManager.INSTANCE.send(PacketDistributor.ALL.noArg(),
                        new SyncLegionColorPacket(serverPlayer.getUUID(), rgb));
            }
        }
    }

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

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        // 初始化高生命值生物数据库
        HighHealthDatabase.init(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        HighHealthDatabase.close();
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
            if (MobControlledData.isSummoned(mob)) return;
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

        // 骑乘令左键处理
        if (mainHand.getItem() instanceof RideCommandItem && !player.level().isClientSide) {
            if (RideCommandItem.handleLeftClick((ServerPlayer) player, target, mainHand)) {
                event.setCanceled(true);
                return;
            }
        }

        // 护主切换器单体切换（左键触发）
        else if (mainHand.getItem() instanceof AggressiveSwitchItem) {
            if (target instanceof Mob mob && !player.level().isClientSide) {
                // 潜行时不处理单体（由鼠标事件处理批量）
                if (!player.isShiftKeyDown()) {
                    if (MobControlledData.isControlledEntity(mob) &&
                            Objects.equals(MobControlledData.getControllerUUID(mob), player.getUUID())) {
                        // 取消伤害
                        event.setCanceled(true);
                        // 发送单体切换包（索敌模式 = true）
                        NetWorkManager.INSTANCE.sendToServer(new SwitchAggressiveModePacket(true, mob.getId()));
                        return;
                    }
                }
            }
        }

        // 控制令·切换模式版左键切换模式
        else if (mainHand.getItem() instanceof ModeSelectControlCommandItem) {
            if (!player.level().isClientSide) {
                ModeSelectControlCommandItem.cycleSelectedMode(mainHand);
                MobControlledData.ControlMode newMode = ModeSelectControlCommandItem.getSelectedMode(mainHand);
                String modeKey = "mob_controller.mode." + newMode.toString().toLowerCase();
                player.displayClientMessage(Component.translatable("mob_controller.message.mode_switched",
                        Component.translatable(modeKey)).withStyle(ChatFormatting.GOLD), true);
            }
            event.setCanceled(true);
        }

        // 竞技之旗左键生物：单体/批量启用军团模式
        else if (mainHand.getItem() instanceof LegionBannerItem) {
            if (target instanceof Mob mob && !player.level().isClientSide) {
                if (MobControlledData.isControlledEntity(mob) &&
                        player.getUUID().equals(MobControlledData.getControllerUUID(mob))) {
                    if (player.isShiftKeyDown()) {
                        // 潜行 + 左键 -> 批量启用军团模式
                        int count = MobControlledData.setLegionModeForAll(player, 32, true);
                        String key = "mob_controller.message.legion_enable_batch";
                        player.displayClientMessage(Component.translatable(key, count).withStyle(ChatFormatting.GOLD), true);
                    } else {
                        // 单体启用军团模式
                        MobControlledData.setLegionMode(mob, true);
                        player.displayClientMessage(Component.translatable("mob_controller.message.legion_enable_single",
                                mob.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
                    }
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onAttackEntityWithAggressiveSwitch(AttackEntityEvent event) {
        // 已合并到 onAttackEntity 中，此方法保留为空或删除均可，但为避免冲突保留原结构
        // 实际逻辑已移至主分支
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
                } else {
                    if (event.getSource().getMsgId().equals("outOfWorld")) {
                        MobControlledData.markCombat(mob);
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
     * 军团模式下的生物会跳过此逻辑（由 onLegionModeTick 单独处理）。
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
        // 军团模式生物由单独的逻辑处理攻击行为，跳过索敌模式
        if (MobControlledData.isLegionMode(mob)) {
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
     * 每刻处理受控生物的军团模式攻击逻辑。
     * 军团模式生物会主动攻击其他不同队伍颜色的军团生物。
     */
    @SubscribeEvent
    public static void onLegionModeTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (mob.level().isClientSide) return;
        if (!MobControlledData.isControlledEntity(mob)) return;
        if (!MobControlledData.isLegionMode(mob)) return;

        // 如果当前是系统攻击（玩家指令），则保留目标，不进行军团自动扫描
        if (MobControlledData.isSystemAttack(mob)) {
            return;
        }

        // 冷却：每20 tick扫描一次
        if (mob.tickCount % 20 != 0) return;

        LivingEntity currentTarget = mob.getTarget();

        // 如果当前目标有效且仍然是军团敌对目标，则保持
        if (currentTarget != null && currentTarget.isAlive() && isLegionEnemy(mob, currentTarget)) {
            return;
        }

        // 清除无效目标
        if (currentTarget != null) {
            mob.setTarget(null);
            if (mob instanceof AbstractPiglin || mob instanceof Hoglin || mob instanceof Zoglin) {
                mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                mob.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
            }
        }

        // 扫描范围
        double range = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (range < 32) range = 32;
        AABB searchArea = mob.getBoundingBox().inflate(range, 4.0, range);
        List<LivingEntity> candidates = mob.level().getEntitiesOfClass(LivingEntity.class, searchArea,
                target -> target.isAlive() && isLegionEnemy(mob, target));
        if (!candidates.isEmpty()) {
            candidates.sort(Comparator.comparingDouble(mob::distanceToSqr));
            LivingEntity bestTarget = candidates.get(0);
            MobControlledData.markSystemAttack(mob);
            // 使用通用目标设置方法
            if (mob instanceof Hoglin || mob instanceof Zoglin) {
                Brain<?> brain = mob.getBrain();
                brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, bestTarget, 200L);
            } else if (mob instanceof Piglin || mob instanceof PiglinBrute) {
                Brain<?> brain = mob.getBrain();
                brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                brain.setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, bestTarget.getUUID(), 600L);
                brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, bestTarget, 200L);
            } else {
                MobControlUtil.setMobTargetWithAnger(mob, bestTarget);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // 1. 将当前玩家的队伍颜色和军团模式发送给所有在线玩家
            ChatFormatting newPlayerColor = LegionBannerItem.getLegionColor(serverPlayer);
            int newPlayerRgb = LegionBannerItem.getColorRGB(newPlayerColor);
            NetWorkManager.INSTANCE.send(PacketDistributor.ALL.noArg(),
                    new SyncLegionColorPacket(serverPlayer.getUUID(), newPlayerRgb));

            boolean newPlayerLegionMode = LegionBannerItem.isPlayerInLegionMode(serverPlayer);
            NetWorkManager.INSTANCE.send(PacketDistributor.ALL.noArg(),
                    new SyncLegionModePacket(serverPlayer.getUUID(), newPlayerLegionMode));

            // 2. 将其他在线玩家的队伍颜色和军团模式发送给当前玩家
            for (ServerPlayer other : serverPlayer.server.getPlayerList().getPlayers()) {
                if (other == serverPlayer) continue;
                ChatFormatting otherColor = LegionBannerItem.getLegionColor(other);
                int otherRgb = LegionBannerItem.getColorRGB(otherColor);
                NetWorkManager.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new SyncLegionColorPacket(other.getUUID(), otherRgb));

                boolean otherLegionMode = LegionBannerItem.isPlayerInLegionMode(other);
                NetWorkManager.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new SyncLegionModePacket(other.getUUID(), otherLegionMode));
            }
        }
    }

    /**
     * 判断军团模式下目标是否为敌方。
     * 条件：目标为军团模式生物，控制者不同，且控制者的队伍颜色不同。
     */
    private static boolean isLegionEnemy(Mob attacker, LivingEntity target) {
        if (MobControlledData.isSystemAttack(attacker) && attacker.getTarget() == target) {
            return true;
        }
        UUID attackerController = MobControlledData.getControllerUUID(attacker);
        if (attackerController == null) return false;

        // 情况1：目标为军团模式生物
        if (target instanceof Mob targetMob && MobControlledData.isLegionMode(targetMob)) {
            UUID targetController = MobControlledData.getControllerUUID(targetMob);
            if (targetController == null) return false;
            if (attackerController.equals(targetController)) return false;

            Player attackerOwner = MobControlledData.getController(attacker, attacker.level());
            Player targetOwner = MobControlledData.getController(targetMob, targetMob.level());
            if (attackerOwner == null || targetOwner == null) return false;
            ChatFormatting attackerColor = LegionBannerItem.getLegionColor(attackerOwner);
            ChatFormatting targetColor = LegionBannerItem.getLegionColor(targetOwner);
            return attackerColor != targetColor;
        }

        // 情况2：目标为军团模式玩家
        if (target instanceof Player targetPlayer && LegionBannerItem.isPlayerInLegionMode(targetPlayer)) {
            // 攻击者必须是受控生物
            if (!MobControlledData.isControlledEntity(attacker)) return false;
            // 玩家不能攻击自己的主人
            if (targetPlayer.getUUID().equals(attackerController)) return false;
            // 获取控制者的队伍颜色与玩家的队伍颜色比较
            Player attackerOwner = MobControlledData.getController(attacker, attacker.level());
            if (attackerOwner == null) return false;
            ChatFormatting attackerColor = LegionBannerItem.getLegionColor(attackerOwner);
            ChatFormatting targetColor = LegionBannerItem.getLegionColor(targetPlayer);
            return attackerColor != targetColor;
        }

        return false;
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
                                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> mob),
                                    new MobControlCapabilitySyncPacket(mob.getId(), cap.serializeNBT())
                            ));
                }
            }
        }
    }

    // 修改原有的 onPlayerRightClickControlledMob 方法，新增对 ModeSelectControlCommandItem 和 LegionBannerItem 的支持
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
        // 控制令·切换模式版（驭兽哨）逻辑
        else if (mainHand.is(ModItems.MODE_SELECT_CONTROL_COMMAND_ITEM.get())) {
            int button = event.getButton();
            if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                ModeSelectControlCommandItem.cycleSelectedMode(mainHand);
                MobControlledData.ControlMode newMode = ModeSelectControlCommandItem.getSelectedMode(mainHand);
                // 发送同步包到服务端
                NetWorkManager.INSTANCE.sendToServer(new SyncSelectedModePacket(newMode));
                String modeKey = "mob_controller.mode." + newMode.toString().toLowerCase();
                mc.player.displayClientMessage(Component.translatable("mob_controller.message.mode_switched",
                        Component.translatable(modeKey)).withStyle(ChatFormatting.GOLD), true);
                mc.player.swing(InteractionHand.MAIN_HAND);
            } else if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
                // 右键：发送批量控制包（服务端执行）
                MobControlledData.ControlMode mode = ModeSelectControlCommandItem.getSelectedMode(mainHand);
                NetWorkManager.INSTANCE.sendToServer(new ApplyControlCommandPacket(mode));
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        }
        // 竞技之旗逻辑
        else if (mainHand.getItem() instanceof LegionBannerItem) {
            int button = event.getButton();
            boolean isSneaking = mc.player.isShiftKeyDown();

            // 如果有实体目标，则交由 interactLivingEntity 和 onAttackEntity 处理，此处不重复
            if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY) {
                return;
            }

            // 对空气点击：切换颜色或批量切换军团模式
            if (isSneaking) {
                // 潜行+左键 -> 批量启用军团模式；潜行+右键 -> 批量禁用军团模式
                if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                    NetWorkManager.INSTANCE.sendToServer(new LegionModeBatchPacket(true));
                } else if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
                    NetWorkManager.INSTANCE.sendToServer(new LegionModeBatchPacket(false));
                } else {
                    return;
                }
            } else {
                // 非潜行：左键向前循环队伍颜色，右键向后循环
                if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                    NetWorkManager.INSTANCE.sendToServer(new UpdateLegionColorPacket(1));
                } else if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
                    NetWorkManager.INSTANCE.sendToServer(new UpdateLegionColorPacket(-1));
                } else if (button == InputConstants.MOUSE_BUTTON_MIDDLE) {
                    NetWorkManager.INSTANCE.sendToServer(new net.xiaoyu.mob_controller.network.TogglePlayerLegionModePacket());
                } else {
                    return;
                }
            }
            mc.player.swing(InteractionHand.MAIN_HAND);
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
                        || mainHandItem.is(ModItems.LEGION_BANNER_ITEM.get())
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

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MobControllerTriggers.CRAFT_CONTROLLER.trigger(player, event.getCrafting());
            MobController.grantRootAdvancementIfNeeded(player);
        }
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
        // 掠夺者弩箭修复
        if (event.getEntity() instanceof Pillager pillager && !event.getEntity().level().isClientSide) {
            if (MobControlledData.isControlledEntity(pillager)) {
                ItemStack mainHand = pillager.getMainHandItem();
                if (mainHand.is(Items.CROSSBOW) && mainHand.isDamaged()) {
                    mainHand.setDamageValue(0);
                }
            }
        }

        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!MobControlledData.isControlledEntity(mob)) return;

        // 天境虚空传送
        if (net.minecraftforge.fml.ModList.get().isLoaded("aether")) {
            handleAetherVoidTeleport(mob);
        }

        // WANDER 模式后备随机游走
        if (MobControlledData.getControlMode(mob) == MobControlledData.ControlMode.WANDER) {
            if (mob.getTarget() != null || mob.getNavigation().isInProgress()) {
                return;
            }

            CompoundTag data = mob.getPersistentData();
            int lastWanderTick = data.getInt("mob_controller_last_wander_tick");
            int currentTick = mob.tickCount;
            int cooldown = 160;

            if (currentTick - lastWanderTick > cooldown) {
                data.putInt("mob_controller_last_wander_tick", currentTick);

                net.minecraft.util.RandomSource random = mob.getRandom();
                double range = 8.0;
                double minRange = 5.0;
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = minRange + random.nextDouble() * (range - minRange);
                double dx = Math.cos(angle) * distance;
                double dz = Math.sin(angle) * distance;
                double x = mob.getX() + dx;
                double z = mob.getZ() + dz;
                double y = mob.getY();

                BlockPos targetPos = new BlockPos((int) x, (int) y, (int) z);
                if (mob.level().isEmptyBlock(targetPos) || mob.level().getFluidState(targetPos).isSource()) {
                    mob.getNavigation().moveTo(x, y, z, 0.8);
                } else {
                    BlockPos above = targetPos.above();
                    if (mob.level().isEmptyBlock(above)) {
                        mob.getNavigation().moveTo(above.getX() + 0.5, above.getY(), above.getZ() + 0.5, 0.8);
                    }
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


    private static void handleAetherVoidTeleport(Mob mob) {
        ServerLevel currentLevel = (ServerLevel) mob.level();
        // 判断是否为天境维度（使用天境模组的工具类，编译时存在）
        if (!currentLevel.dimension().equals(com.aetherteam.aether.world.LevelUtil.destinationDimension())) {
            return;
        }

        // 检查 Y 坐标是否低于世界最低高度
        if (mob.getY() > currentLevel.getMinBuildHeight()) return;

        // 检查生物群系是否允许掉落传送（使用天境模组的 Tag）
        var biomeTag = net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.BIOME,
                new net.minecraft.resources.ResourceLocation("aether", "fall_to_overworld")
        );
        if (!currentLevel.getBiome(mob.blockPosition()).is(biomeTag)) return;

        // 获取目标维度（主世界，使用天境模组的返回维度工具）
        var destinationKey = com.aetherteam.aether.world.LevelUtil.returnDimension();
        ServerLevel targetLevel = currentLevel.getServer().getLevel(destinationKey);
        if (targetLevel == null) return;

        // 获取当前实体的所有乘客
        java.util.List<net.minecraft.world.entity.Entity> passengers = mob.getPassengers();

        // 执行维度传送（使用天境模组的 AetherPortalForcer）
        net.minecraft.world.entity.Entity newEntity = mob.changeDimension(
                targetLevel,
                new com.aetherteam.aether.block.portal.AetherPortalForcer(targetLevel, false)
        );

        if (newEntity instanceof Mob newMob) {
            // 将乘客重新骑乘到新实体上
            for (net.minecraft.world.entity.Entity passenger : passengers) {
                passenger.stopRiding();
                net.minecraft.world.entity.Entity newPassenger = passenger.changeDimension(targetLevel, new com.aetherteam.aether.block.portal.AetherPortalForcer(targetLevel, false));
                if (newPassenger != null) {
                    newPassenger.startRiding(newMob, true);
                    // 若乘客是玩家，设置传送计时器（防止飞行检测）
                    if (newPassenger instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        com.aetherteam.aether.event.hooks.DimensionHooks.teleportationTimer = 500;
                    }
                }
            }
        }
    }

    /**
     * 当重生开启且配置允许时，阻止受控生物掉落物品。
     */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;

        // 仅当重生功能开启且配置要求阻止掉落时生效
        if (Config.ENABLE_RESPAWN.get() && FeatureConfig.PREVENT_DROPS_ON_RESPAWN.get()) {
            if (MobControlledData.isControlledEntity(mob)) {
                event.getDrops().clear();   // 清空所有掉落物
            }
        }
    }

    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (Config.ENABLE_RESPAWN.get() && FeatureConfig.PREVENT_DROPS_ON_RESPAWN.get()) {
            if (MobControlledData.isControlledEntity(mob)) {
                event.setDroppedExperience(0); // 取消经验掉落
            }
        }
    }

    @SubscribeEvent
    public static void onEntityTeleport(EntityTeleportEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Mob mob)) return;
        if (!MobControlledData.isControlledEntity(mob)) return;
        UUID controllerUUID = MobControlledData.getControllerUUID(mob);
        if (controllerUUID == null) return;

        // 更新数据库中的维度、坐标
        CompoundTag partialNbt = new CompoundTag(); // 只更新位置和维度，NBT 主体可以留空或仅保存必要字段
        partialNbt.putString("dimension", mob.level().dimension().location().toString());
        partialNbt.putInt("x", mob.getBlockX());
        partialNbt.putInt("y", mob.getBlockY());
        partialNbt.putInt("z", mob.getBlockZ());
        HighHealthDatabase.updateMobData(controllerUUID, mob, partialNbt);
    }
}