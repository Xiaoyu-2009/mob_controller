package net.xiaoyu.mob_controller.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.capability.MobControlCapability;
import net.xiaoyu.mob_controller.capability.MobControlCapabilityProvider;
import net.xiaoyu.mob_controller.item.LegionBannerItem;
import net.xiaoyu.mob_controller.network.MobControlCapabilitySyncPacket;
import net.xiaoyu.mob_controller.network.NetWorkManager;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 维护“被控制生物”的运行时数据与全局辅助逻辑。
 *
 * <p>该类负责：</p>
 * <ul>
 *   <li>记录玩家已控制的高生命值生物类型，限制同类重复控制；</li>
 *   <li>读写生物控制状态（控制者、模式、系统攻击标记、索敌模式标记）；</li>
 *   <li>安排并处理生物死亡后的延迟重生（同时记录死因）。</li>
 * </ul>
 */
public class MobControlledData {
    /**
     * 待执行的延迟重生任务。键为死亡生物 UUID。
     */
    private static final Map<UUID, PendingRespawnData> PENDING_RESPAWNS = new ConcurrentHashMap<>();
    private static final String PENDING_RESPAWN_TAG = "pending_respawns";
    private static final String PENDING_RESPAWN_DATA_DIR = "mob_controller";
    private static final String PENDING_RESPAWN_FILE = "pending_respawns.dat";
    @Nullable
    private static Path loadedPendingRespawnFile;

    /**
     * 受批量控制指令影响的生物发光持续时间（刻）。
     */
    private static final int AFFECTED_MOB_GLOWING_TICKS = 100;

    /**
     * 将生物加入控制状态，并初始化为“跟随”模式。
     *
     * @param controllerUUID 控制者玩家 UUID
     * @param mob            目标生物
     */
    public static void addControlledMob(UUID controllerUUID, Mob mob, boolean setPersistent, boolean skipHighHealthRecord) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        capability.ifPresent(cap -> {
            cap.setControllerUUID(controllerUUID);
            cap.setControlMode(ControlMode.FOLLOW);
            cap.setLastHealTime(0L);
            cap.setLastCombatTime(0L);
            cap.setSystemAttack(false);
            cap.setAggressiveMode(false);
        });

        if (setPersistent) {
            mob.setPersistenceRequired();
        }
        if (!(mob instanceof Panda) && !(mob instanceof Piglin)) {
            mob.setCanPickUpLoot(false);
        }

        if (!skipHighHealthRecord) {
            addHighHealthRecord(controllerUUID, mob);
        }
    }

    // 原有的双参数方法保持兼容，默认不跳过记录
    public static void addControlledMob(UUID controllerUUID, Mob mob, boolean setPersistent) {
        addControlledMob(controllerUUID, mob, setPersistent, false);
    }

    public static void addControlledMob(UUID controllerUUID, Mob mob) {
        addControlledMob(controllerUUID, mob, true, false);
    }



    /**
     * 释放对生物的控制并清理相关标记。
     *
     * @param mob 要释放的生物
     * @return {@code true} 表示该生物存在控制能力并已执行释放流程
     */
    public static boolean releaseControl(Mob mob) {
        UUID controllerUUID = getControllerUUID(mob);
        if (controllerUUID == null) {
            return false;
        }

        setLegionMode(mob, false);

        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        capability.ifPresent(cap -> {
            cap.setControllerUUID(null);
            cap.setControlMode(ControlMode.FOLLOW);
            cap.setLastHealTime(0L);
            cap.setLastCombatTime(0L);
            cap.setSystemAttack(false);
            cap.setAggressiveMode(false);
        });

        if (!mob.level().isClientSide) {
            capability.ifPresent(cap -> {
                NetWorkManager.INSTANCE.send(
                        net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> mob),
                        new net.xiaoyu.mob_controller.network.MobControlCapabilitySyncPacket(mob.getId(), cap.serializeNBT())
                );
            });
        }

        if (mob instanceof Raider raider && !(mob instanceof Witch)) {
            MobControlUtil.restoreRaiderTargets(raider);
        }
        if (mob instanceof Witch witch) {
            MobControlUtil.restoreWitchTargets(witch);
        }

        removeHighHealthRecord(controllerUUID, mob);
        return capability.isPresent();
    }

    private static void resetWitchToVanilla(Witch witch) {
        witch.getPersistentData().remove("mob_controller.supportCooldown");
    }

    public static boolean isHighHealthMob(Mob mob) {
        return mob.getMaxHealth() > Config.HIGH_HEALTH_THRESHOLD.get();
    }

    /**
     * 判断玩家是否已经控制过同类型的高生命值生物。
     *
     * @param playerUUID 玩家 UUID
     * @param mob        准备控制的目标生物
     * @return 若目标为高生命值生物且该玩家已控制同类型生物则返回 {@code true}
     */
    public static boolean hasPlayerControlledSameHighHealthMob(UUID playerUUID, Mob mob) {
        String typeId = EntityType.getKey(mob.getType()).toString();
        int customMax = MobControlUtil.getCustomMaxCount(mob);
        if (customMax >= 0) {
            // 配置中明确写了0或正数
            return !HighHealthDatabase.canControlMore(playerUUID, typeId, customMax);
        } else if (customMax == -1) {
            // 配置中明确写了-1（无限制）
            return false;
        } else {
            // 未配置，使用默认高生命值限制
            if (!isHighHealthMob(mob)) return false;
            return !HighHealthDatabase.canControlMore(playerUUID, typeId, 1);
        }
    }

    /**
     * 在被控制生物死亡时移除高生命值控制记录。
     *
     * @param mob 死亡生物
     */
    public static void removeControlledMobOnDeath(Mob mob) {
        UUID controllerUUID = getControllerUUID(mob);
        if (controllerUUID == null) return;

        boolean willRespawn = PENDING_RESPAWNS.containsKey(mob.getUUID());

        if (!willRespawn) {
            removeHighHealthRecord(controllerUUID, mob);
        }
    }

    private static void removeHighHealthRecord(UUID controllerUUID, Mob mob) {
        HighHealthDatabase.deleteRecord(controllerUUID, mob.getUUID());
    }

    private static void addHighHealthRecord(UUID controllerUUID, Mob mob) {
        String typeId = EntityType.getKey(mob.getType()).toString();
        int customMax = MobControlUtil.getCustomMaxCount(mob);

        int maxAllowed;
        if (customMax >= 0) {
            maxAllowed = customMax;
        } else if (customMax == -1) {
            maxAllowed = -1;  // 无限制
        } else {
            // 未配置，高生命值生物上限1
            if (!isHighHealthMob(mob)) return;
            maxAllowed = 1;
        }

        if (!HighHealthDatabase.canControlMore(controllerUUID, typeId, maxAllowed)) {
            throw new IllegalStateException("Cannot control more than " + maxAllowed + " of " + typeId);
        }

        CompoundTag fullNbt = mob.saveWithoutId(new CompoundTag());
        boolean success = HighHealthDatabase.insertControlledMob(controllerUUID, mob, fullNbt);
        if (!success) {
            throw new IllegalStateException("Failed to insert controlled mob record");
        }
    }

    private static Optional<EntityType<?>> getPendingMobType(String typeId) {
        ResourceLocation location = ResourceLocation.tryParse(typeId);
        if (location == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ForgeRegistries.ENTITY_TYPES.getValue(location));
    }

    // 列表中移除[被控制的生物死亡]

    /**
     * 判断生物是否处于被控制状态。
     *
     * @param mob 生物实体
     * @return {@code true} 表示存在控制者
     */
    public static boolean isControlledEntity(LivingEntity mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        return capability.map(MobControlCapability::isControlled).orElse(false);
    }

    /**
     * 获取生物的控制者 UUID。
     *
     * @param mob 生物实体
     * @return 控制者 UUID；若未被控制则返回 {@code null}
     */
    public static @Nullable UUID getControllerUUID(LivingEntity mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        return capability.resolve().map(MobControlCapability::getControllerUUID).orElse(null);
    }

    /**
     * 在给定维度内查找生物对应的控制者玩家对象。
     *
     * @param mob   生物实体
     * @param level 查询所用世界
     * @return 控制者玩家；未找到时返回 {@code null}
     */
    @Nullable
    public static Player getController(LivingEntity mob, Level level) {
        UUID controllerUUID = MobControlledData.getControllerUUID(mob);
        if (controllerUUID != null) {
            for (Player player : level.players()) {
                if (player.getUUID().equals(controllerUUID)) {
                    return player;
                }
            }
        }

        return null;
    }

    public static @Nullable String getControllerName(LivingEntity mob, Level level) {
        Player controller = MobControlledData.getController(mob, level);
        if (controller != null) {
            return controller.getName().getString();
        }
        MinecraftServer server = level.getServer();
        UUID uuid = MobControlledData.getControllerUUID(mob);
        if (server == null || uuid == null) {
            return null;
        }
        GameProfileCache profileCache = server.getProfileCache();
        if (profileCache == null) {
            return null;
        }
        Optional<GameProfile> gameProfile = profileCache.get(uuid);
        if (gameProfile.isEmpty()) {
            return null;
        }
        GameProfile profile = gameProfile.get();
        return profile.getName();
    }

    /**
     * 设置生物的控制模式。
     *
     * @param mob  生物实体
     * @param mode 目标控制模式
     */
    public static void setControlMode(Mob mob, ControlMode mode) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        capability.ifPresent(cap -> cap.setControlMode(mode));
        if (mode == ControlMode.STAY) {
            mob.getNavigation().stop();
            mob.getNavigation().createPath(mob.blockPosition(), 10);
        }
    }

    /**
     * 获取生物最近一次交战时间。
     *
     * @param mob 生物实体
     * @return 最近交战的游戏时间刻；若能力缺失则返回 {@code 0L}
     */
    public static long getLastCombatTime(Mob mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        return capability.map(MobControlCapability::getLastCombatTime).orElse(0L);
    }

    /**
     * 记录生物最近一次交战时间。
     *
     * @param mob  生物实体
     * @param time 当前游戏时间刻
     */
    public static void setLastCombatTime(Mob mob, long time) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        capability.ifPresent(cap -> cap.setLastCombatTime(time));
    }

    /**
     * 以当前世界时间记录一次交战。
     *
     * @param mob 生物实体
     */
    public static void markCombat(Mob mob) {
        setLastCombatTime(mob, mob.level().getGameTime());
    }

    /**
     * 获取生物当前控制模式。
     *
     * @param mob 生物实体
     * @return 当前模式；若能力缺失则回退为 {@link ControlMode#FOLLOW}
     */
    public static ControlMode getControlMode(Mob mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        return capability.map(MobControlCapability::getControlMode).orElse(ControlMode.FOLLOW);
    }

    /**
     * 按顺序循环切换控制模式（跟随 -> 停留 -> 游荡 -> 跟随）。
     *
     * @param mob 生物实体
     * @return 切换后的新模式
     */
    public static ControlMode toggleControlMode(Mob mob) {
        ControlMode currentMode = getControlMode(mob);
        int index = currentMode.ordinal() + 1;
        ControlMode newMode = ControlMode.values()[index >= ControlMode.values().length ? 0 : index];
        setControlMode(mob, newMode);
        return newMode;
    }

    /**
     * 标记该生物当前攻击为系统触发。
     *
     * @param mob 生物实体
     */
    public static void markSystemAttack(Mob mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        capability.ifPresent(cap -> cap.setSystemAttack(true));
    }

    /**
     * 清除系统攻击标记。
     *
     * @param mob 生物实体
     */
    public static void clearSystemAttack(Mob mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        capability.ifPresent(cap -> cap.setSystemAttack(false));
    }

    /**
     * 查询系统攻击标记。
     *
     * @param mob 生物实体
     * @return {@code true} 表示当前攻击被标记为系统触发
     */
    public static boolean isSystemAttack(Mob mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        return capability.map(MobControlCapability::isSystemAttack).orElse(false);
    }

    // ========== 索敌模式相关方法 ==========

    /**
     * 获取生物的索敌模式状态。
     *
     * @param mob 生物实体
     * @return {@code true} 表示当前为索敌模式，{@code false} 表示护主模式
     */
    public static boolean isAggressiveMode(Mob mob) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        return capability.map(MobControlCapability::isAggressiveMode).orElse(false);
    }

    /**
     * 设置生物的索敌模式状态。
     *
     * @param mob    生物实体
     * @param aggressive 索敌模式标记
     */
    public static void setAggressiveMode(Mob mob, boolean aggressive) {
        LazyOptional<MobControlCapability> capability = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY);
        capability.ifPresent(cap -> cap.setAggressiveMode(aggressive));
    }

    /**
     * 批量设置玩家周围指定半径内所有受控生物的索敌模式。
     * <p>受影响的生物会获得短暂的发光效果（持续 100 tick），与控制令的行为一致。</p>
     *
     * @param player     操作玩家
     * @param radius     半径（方块）
     * @param aggressive 索敌模式（true=索敌，false=护主）
     * @return 受影响生物数量
     */
    public static int setAggressiveModeForAll(Player player, int radius, boolean aggressive) {
        if (player.level().isClientSide) {
            return 0;
        }

        AABB area = player.getBoundingBox().inflate(radius);
        List<Mob> controlledMobs = player.level().getEntitiesOfClass(
                Mob.class, area, mob ->
                        MobControlledData.isControlledEntity(mob) && player.getUUID().equals(MobControlledData.getControllerUUID(mob))
        );

        for (Mob mob : controlledMobs) {
            setAggressiveMode(mob, aggressive);
            mob.setTarget(null); // 清除当前目标，避免残留仇恨
            // 添加发光效果，与“控制令”行为保持一致
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, AFFECTED_MOB_GLOWING_TICKS));
        }
        return controlledMobs.size();
    }

    // ========== 召唤物标记 ==========
    public static boolean isSummoned(Mob mob) {
        return mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY)
                .map(MobControlCapability::isSummoned).orElse(false);
    }

    // ========== 重生相关方法 ==========

    /**
     * 为死亡生物创建延迟重生任务（新版本，支持死因）。
     *
     * <p>会保存实体 NBT 与能力 NBT，在 {@link #tickPendingRespawns(MinecraftServer)} 中恢复。</p>
     *
     * @param mob        死亡生物
     * @param level      当前服务端世界
     * @param deathCause 死因描述文本（纯文本，不包含生物名称）
     * @return {@code true} 表示成功加入待重生队列
     */
    public static boolean scheduleRespawn(Mob mob, ServerLevel level, String deathCause) {
        if (isSummoned(mob)) {
            return false;
        }
        MinecraftServer server = level.getServer();
        ensurePendingRespawnsLoaded(server);

        if (mob instanceof Slime slime) {
            if (MobControlledData.isSplitOffspring(mob)) {
                return false;
            }
        }

        UUID controllerUUID = getControllerUUID(mob);
        if (controllerUUID == null || PENDING_RESPAWNS.containsKey(mob.getUUID())) {
            return false;
        }

        CompoundTag entityNbt = mob.saveWithoutId(new CompoundTag());
        entityNbt.putString("id", EntityType.getKey(mob.getType()).toString());

        CompoundTag capabilityNbt = mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY)
                .map(MobControlCapability::serializeNBT)
                .orElse(new CompoundTag());

        String mobTypeId = EntityType.getKey(mob.getType()).toString();
        boolean highHealthMob = isHighHealthMob(mob);

        // ★ 新增：标记数据库中的记录为“重生等待中” ★
        HighHealthDatabase.markAsRespawning(controllerUUID, mob.getUUID());

        PENDING_RESPAWNS.put(
                mob.getUUID(), new PendingRespawnData(
                        mob.getUUID(),
                        controllerUUID,
                        entityNbt,
                        capabilityNbt,
                        server.getTickCount() + Config.RESPAWN_DELAY_TICKS.get(),
                        level.dimension(),
                        mob.blockPosition(),
                        mobTypeId,
                        highHealthMob,
                        deathCause != null ? deathCause : ""
                )
        );
        savePendingRespawns(server);
        return true;
    }

    /**
     * 每刻处理待重生队列，时间到达后尝试生成并恢复生物状态。
     *
     * @param server 当前服务端实例
     */
    public static void tickPendingRespawns(MinecraftServer server) {
        ensurePendingRespawnsLoaded(server);
        int currentTick = server.getTickCount();
        Set<UUID> completedRespawns = new HashSet<>();

        for (Map.Entry<UUID, PendingRespawnData> entry : PENDING_RESPAWNS.entrySet()) {
            PendingRespawnData data = entry.getValue();
            if (data.triggerTick() > currentTick) {
                continue;
            }

            ServerPlayer controller = server.getPlayerList().getPlayer(data.controllerUUID());
            ServerLevel targetLevel = controller != null ? controller.serverLevel() : server.getLevel(data.deathDimension());

            if (targetLevel == null) {
                continue;
            }

            CompoundTag nbt = data.entityNbt().copy();
            Optional<Entity> createdEntity = EntityType.create(nbt, targetLevel);
            if (createdEntity.isPresent() && createdEntity.get() instanceof Mob respawnedMob) {
                if (controller != null) {
                    respawnedMob.moveTo(
                            controller.getX(),
                            controller.getY(),
                            controller.getZ(),
                            respawnedMob.getYRot(),
                            respawnedMob.getXRot()
                    );
                } else {
                    respawnedMob.moveTo(
                            data.deathPos().getX() + 0.5D, data.deathPos().getY(), data.deathPos().getZ() + 0.5D,
                            respawnedMob.getYRot(), respawnedMob.getXRot()
                    );
                }

                respawnedMob.setDeltaMovement(0, 0, 0);
                respawnedMob.setHealth(respawnedMob.getMaxHealth());
                respawnedMob.setTarget(null);
                targetLevel.addFreshEntity(respawnedMob);
                respawnedMob.fallDistance = 0.0f;

                if (MobControlledData.isControlledEntity(respawnedMob) && respawnedMob instanceof Warden) {
                    respawnedMob.getPersistentData().putBoolean("mob_controller:respawned", true);
                }

                resetBossPhaseIfNeeded(respawnedMob);

                // ★ 重生：添加控制时跳过高生命记录（数据库已在 markAsRespawning 中保留记录）
                addControlledMob(data.controllerUUID(), respawnedMob, true, true);

                // ★ 更新数据库：将旧的标记记录更新为新实体的信息 ★
                CompoundTag newNbt = respawnedMob.saveWithoutId(new CompoundTag());
                HighHealthDatabase.respawnCompleted(data.controllerUUID(), data.deadMobUUID(), respawnedMob, newNbt);

                respawnedMob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY)
                        .ifPresent(cap -> cap.deserializeNBT(data.capabilityNbt().copy()));
                clearSystemAttack(respawnedMob);

                MobControlUtil.clearCombatMemory(respawnedMob);

                if (controller != null) {
                    controller.sendSystemMessage(Component.translatable("mob_controller.message.respawned", respawnedMob.getDisplayName()));
                }
            }

            completedRespawns.add(entry.getKey());
        }

        if (!completedRespawns.isEmpty()) {
            for (UUID deadMobUUID : completedRespawns) {
                PENDING_RESPAWNS.remove(deadMobUUID);
            }
            savePendingRespawns(server);
        }
    }

    private static void resetBossPhaseIfNeeded(Mob mob) {
        resetCataclysmBossPhase(mob);
        resetTwilightForestBossPhase(mob);
    }


    /**
     * 重置灾变（Cataclysm）模组的 Boss 阶段状态
     */
    private static void resetCataclysmBossPhase(Mob mob) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("cataclysm")) return;

        // Ender Guardian
        if (mob instanceof com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.Ender_Guardian_Entity guardian) {
            guardian.setIsHelmetless(false);
            guardian.setUsedMassDestruction(false);
        }
        // Netherite Monstrosity
        else if (mob instanceof com.github.L_Ender.cataclysm.entity.InternalAnimationMonster.IABossMonsters.NewNetherite_Monstrosity.Netherite_Monstrosity_Entity monstrosity) {
            monstrosity.setIsBerserk(false);
        }
        // The Harbinger
        else if (mob instanceof com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.The_Harbinger_Entity harbinger) {
            harbinger.setIsLaserMode(false);
            harbinger.setOverload(0);
            harbinger.setIsAct(true);
        }
        // Ancient Remnant
        else if (mob instanceof com.github.L_Ender.cataclysm.entity.InternalAnimationMonster.IABossMonsters.Ancient_Remnant.Ancient_Remnant_Entity remnant) {
            remnant.setIsPower(false);
            remnant.setRage(0);
            remnant.setNecklace(true);
        }
        // Scylla
        else if (mob instanceof com.github.L_Ender.cataclysm.entity.InternalAnimationMonster.IABossMonsters.Scylla.Scylla_Entity scylla) {
            scylla.setPhase(0);
            scylla.setEye(false);
            scylla.setAct(true);
            scylla.setChainAnchor(false);
            scylla.setFlying(false);
        }
        // Ignis
        else if (mob instanceof com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.Ignis_Entity ignis) {
            ignis.setBossPhase(0);
            ignis.setIsShieldBreak(false);
            ignis.setShieldDurability(0);
            ignis.setShowShield(true);
            ignis.setIsBlocking(false);
            ignis.setIsSword(false);
        }
        // The Leviathan
        else if (mob instanceof com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.The_Leviathan.The_Leviathan_Entity leviathan) {
            leviathan.setMeltDown(false);
            leviathan.setBlastChance(0);
            leviathan.setModeChance(0);
        }
        // Maledictus
        else if (mob instanceof com.github.L_Ender.cataclysm.entity.InternalAnimationMonster.IABossMonsters.Maledictus.Maledictus_Entity maledictus) {
            maledictus.setRageMeter(0);
            maledictus.setWeapon(0);
        }
    }

    /**
     * 重置暮色森林钟巫妖的护盾
     */
    private static void resetTwilightForestBossPhase(Mob mob) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("twilightforest")) return;
        if (mob instanceof twilightforest.entity.boss.Lich lich) {
            lich.setShieldStrength(6);
        }
    }

    private static void ensurePendingRespawnsLoaded(MinecraftServer server) {
        Path filePath = getPendingRespawnFilePath(server);
        if (!filePath.equals(loadedPendingRespawnFile)) {
            loadPendingRespawns(server, filePath);
            loadedPendingRespawnFile = filePath;
        }
    }

    private static Path getPendingRespawnFilePath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve(PENDING_RESPAWN_DATA_DIR)
                .resolve(PENDING_RESPAWN_FILE);
    }

    private static void loadPendingRespawns(MinecraftServer server, Path filePath) {
        PENDING_RESPAWNS.clear();
        if (!Files.exists(filePath)) {
            return;
        }

        try (InputStream inputStream = Files.newInputStream(filePath)) {
            CompoundTag rootTag = NbtIo.readCompressed(inputStream);
            if (!rootTag.contains(PENDING_RESPAWN_TAG, Tag.TAG_LIST)) {
                return;
            }

            ListTag pendingList = rootTag.getList(PENDING_RESPAWN_TAG, Tag.TAG_COMPOUND);
            for (int i = 0; i < pendingList.size(); i++) {
                CompoundTag respawnTag = pendingList.getCompound(i);
                if (!respawnTag.hasUUID("deadMobUUID") || !respawnTag.hasUUID("controllerUUID")) {
                    continue;
                }

                ResourceLocation dimensionLocation = ResourceLocation.tryParse(respawnTag.getString("deathDimension"));
                if (dimensionLocation == null) {
                    continue;
                }

                UUID deadMobUUID = respawnTag.getUUID("deadMobUUID");
                UUID controllerUUID = respawnTag.getUUID("controllerUUID");
                CompoundTag entityNbt = respawnTag.getCompound("entityNbt");
                CompoundTag capabilityNbt = respawnTag.getCompound("capabilityNbt");
                int remainingTicks = Math.max(0, respawnTag.getInt("remainingTicks"));
                ResourceKey<Level> deathDimension = ResourceKey.create(Registries.DIMENSION, dimensionLocation);
                BlockPos deathPos = BlockPos.of(respawnTag.getLong("deathPos"));
                String deathCause = respawnTag.getString("deathCause");

                PENDING_RESPAWNS.put(
                        deadMobUUID,
                        new PendingRespawnData(
                                deadMobUUID,
                                controllerUUID,
                                entityNbt,
                                capabilityNbt,
                                server.getTickCount() + remainingTicks,
                                deathDimension,
                                deathPos,
                                respawnTag.getString("mobTypeId"),
                                respawnTag.getBoolean("highHealthMob"),
                                deathCause
                        )
                );
            }
        } catch (IOException ignored) {
        }
    }

    private static void savePendingRespawns(MinecraftServer server) {
        Path filePath = getPendingRespawnFilePath(server);
        try {
            Files.createDirectories(filePath.getParent());

            CompoundTag rootTag = new CompoundTag();
            ListTag pendingList = new ListTag();
            int currentTick = server.getTickCount();

            for (PendingRespawnData data : PENDING_RESPAWNS.values()) {
                CompoundTag respawnTag = new CompoundTag();
                respawnTag.putUUID("deadMobUUID", data.deadMobUUID());
                respawnTag.putUUID("controllerUUID", data.controllerUUID());
                respawnTag.put("entityNbt", data.entityNbt().copy());
                respawnTag.put("capabilityNbt", data.capabilityNbt().copy());
                respawnTag.putInt("remainingTicks", Math.max(0, data.triggerTick() - currentTick));
                respawnTag.putString("deathDimension", data.deathDimension().location().toString());
                respawnTag.putLong("deathPos", data.deathPos().asLong());
                respawnTag.putString("mobTypeId", data.mobTypeId());
                respawnTag.putBoolean("highHealthMob", data.highHealthMob());
                respawnTag.putString("deathCause", data.deathCause());
                pendingList.add(respawnTag);
            }

            rootTag.put(PENDING_RESPAWN_TAG, pendingList);

            try (OutputStream outputStream = Files.newOutputStream(filePath)) {
                NbtIo.writeCompressed(rootTag, outputStream);
            }
        } catch (IOException ignored) {
        }
    }

    public static boolean isSplitOffspring(Mob mob) {
        return mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY)
                .map(MobControlCapability::isSplitOffspring)
                .orElse(false);
    }

    public static void setSplitOffspring(Mob mob, boolean value) {
        mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY)
                .ifPresent(cap -> cap.setSplitOffspring(value));
    }

    /**
     * 控制模式。
     */
    public enum ControlMode {
        /**
         * 跟随
         */
        FOLLOW,
        /**
         * 停留
         */
        STAY,
        /**
         * 游荡
         */
        WANDER,
    }

    /**
     * 待重生数据记录。
     *
     * @param deadMobUUID      死亡生物 UUID
     * @param controllerUUID   控制者 UUID
     * @param entityNbt        实体完整 NBT
     * @param capabilityNbt    能力 NBT
     * @param triggerTick      触发重生的服务端 tick 值
     * @param deathDimension   死亡时所在维度
     * @param deathPos         死亡位置
     * @param mobTypeId        实体类型注册 ID
     * @param highHealthMob    是否为高生命值生物
     * @param deathCause       死因描述（纯文本，不含生物名）
     */
    private record PendingRespawnData(
            UUID deadMobUUID, UUID controllerUUID, CompoundTag entityNbt,
            CompoundTag capabilityNbt, int triggerTick,
            ResourceKey<Level> deathDimension,
            BlockPos deathPos,
            String mobTypeId,
            boolean highHealthMob,
            String deathCause
    ) {
    }

    /**
     * 单体切换生物的索敌模式
     *
     * @param player     执行操作的玩家
     * @param mob        目标生物
     * @param aggressive 目标模式（true=索敌，false=护主）
     * @return 是否切换成功
     */
    public static boolean setSingleAggressiveMode(Player player, Mob mob, boolean aggressive) {
        if (!isControlledEntity(mob)) {
            player.displayClientMessage(Component.translatable("mob_controller.message.not_controlled").withStyle(ChatFormatting.RED), true);
            return false;
        }

        if (!player.getUUID().equals(getControllerUUID(mob))) {
            player.displayClientMessage(Component.translatable("mob_controller.message.not_owner").withStyle(ChatFormatting.RED), true);
            return false;
        }

        // 切换模式
        setAggressiveMode(mob, aggressive);

        // 清除当前目标，避免残留仇恨
        mob.setTarget(null);

        // 同步能力数据到客户端
        if (player instanceof ServerPlayer serverPlayer) {
            mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY)
                    .ifPresent(cap -> {
                        NetWorkManager.INSTANCE.send(
                                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> mob),
                                new MobControlCapabilitySyncPacket(mob.getId(), cap.serializeNBT())
                        );
                    });
        }

        // 发送反馈消息
        String modeKey = aggressive ? "mob_controller.mode.aggressive" : "mob_controller.mode.protective";
        player.displayClientMessage(
                Component.translatable("mob_controller.message.single_switch",
                                mob.getDisplayName(),
                                Component.translatable(modeKey))
                        .withStyle(ChatFormatting.GOLD),
                true
        );

        return true;
    }

    // 获取军团模式状态
    public static boolean isLegionMode(Mob mob) {
        return mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY)
                .map(MobControlCapability::isLegionMode).orElse(false);
    }

    // 设置单个生物的军团模式（自动处理aggressive模式强制/恢复）
    public static void setLegionMode(Mob mob, boolean enabled) {
        mob.getCapability(MobControlCapabilityProvider.MOB_CONTROL_CAPABILITY).ifPresent(cap -> {
            cap.setLegionMode(enabled);
            mob.setTarget(null);
            NetWorkManager.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> mob),
                    new MobControlCapabilitySyncPacket(mob.getId(), cap.serializeNBT()));
        });
    }

    // 批量切换（半径以内所有玩家控制的生物）
    public static int setLegionModeForAll(Player player, int radius, boolean enabled) {
        if (player.level().isClientSide) return 0;
        AABB area = player.getBoundingBox().inflate(radius);
        List<Mob> controlledMobs = player.level().getEntitiesOfClass(Mob.class, area,
                mob -> isControlledEntity(mob) && player.getUUID().equals(getControllerUUID(mob)));
        for (Mob mob : controlledMobs) {
            setLegionMode(mob, enabled);
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100));
        }
        return controlledMobs.size();
    }

    /**
     * 当玩家改变队伍颜色后，全局清理所有因颜色相同而不再敌对的军团战斗目标。
     * 包括生物之间的军团目标，以及生物对该玩家的攻击目标。
     * 注意：由玩家直接指挥的攻击（系统攻击）不会被清除。
     *
     * @param changedPlayer 改变颜色的玩家
     */
    public static void clearLegionTargetsAfterPlayerColorChange(Player changedPlayer) {
        if (changedPlayer.level().isClientSide) return;
        ServerLevel level = (ServerLevel) changedPlayer.level();
        UUID changedUUID = changedPlayer.getUUID();

        // 1. 清理生物之间的军团目标
        List<Mob> allControlledMobs = level.getEntitiesOfClass(Mob.class,
                changedPlayer.getBoundingBox().inflate(128),
                mob -> isControlledEntity(mob));

        for (Mob attacker : allControlledMobs) {
            LivingEntity target = attacker.getTarget();
            if (!(target instanceof Mob targetMob)) continue;
            if (!isLegionMode(targetMob)) continue;

            // 如果攻击是由玩家指令发起的（系统攻击），则保留，不清除
            if (isSystemAttack(attacker)) {
                continue;
            }

            Player attackerOwner = getController(attacker, level);
            if (attackerOwner == null) continue;
            UUID targetControllerUUID = getControllerUUID(targetMob);
            if (targetControllerUUID == null) continue;
            Player targetOwner = level.getServer().getPlayerList().getPlayer(targetControllerUUID);
            if (targetOwner == null) continue;

            ChatFormatting attackerColor = LegionBannerItem.getLegionColor(attackerOwner);
            ChatFormatting targetColor = LegionBannerItem.getLegionColor(targetOwner);

            if (attackerColor == targetColor) {
                attacker.setTarget(null);

                if (attacker instanceof AbstractPiglin || attacker instanceof Hoglin || attacker instanceof Zoglin) {
                    attacker.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                    attacker.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
                }

                if (attacker instanceof Warden warden) {
                    Brain<?> brain = warden.getBrain();
                    if (brain != null) {
                        brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
                        brain.eraseMemory(MemoryModuleType.ROAR_TARGET);
                    }
                    warden.clearAnger(targetMob);
                }
            }
        }

        // 2. 清理所有受控生物对该玩家的攻击目标（内部已处理系统攻击跳过）
        clearControlledMobsTargetOnPlayer(changedPlayer);
    }

    public static int getLegionColorRGB(Mob mob) {
        if (!isLegionMode(mob)) return -1;
        Player controller = getController(mob, mob.level());
        if (controller != null) {
            ChatFormatting color = LegionBannerItem.getLegionColor(controller);
            return LegionBannerItem.getColorRGB(color);
        }
        return -1;
    }

    /**
     * 清除所有受控生物对指定玩家的攻击目标（用于玩家颜色改变或军团模式切换时）。
     * 注意：如果生物是由主人指令（系统攻击）攻击该玩家的，则不清除，以尊重玩家指挥。
     */
    public static void clearControlledMobsTargetOnPlayer(Player player) {
        if (player.level().isClientSide) return;
        ServerLevel level = (ServerLevel) player.level();
        List<Mob> allControlled = level.getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(128),
                mob -> MobControlledData.isControlledEntity(mob));
        for (Mob mob : allControlled) {
            if (mob.getTarget() == player) {
                // 如果是由玩家指令发起的攻击（系统攻击），则保留，不清除
                if (MobControlledData.isSystemAttack(mob)) {
                    continue;
                }
                mob.setTarget(null);
                if (mob instanceof AbstractPiglin || mob instanceof Hoglin || mob instanceof Zoglin) {
                    Brain<?> brain = mob.getBrain();
                    brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
                    brain.eraseMemory(MemoryModuleType.ANGRY_AT);
                } else if (mob instanceof Warden warden) {
                    Brain<?> brain = warden.getBrain();
                    if (brain != null) {
                        brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
                        brain.eraseMemory(MemoryModuleType.ROAR_TARGET);
                    }
                    warden.clearAnger(player);
                }
            }
        }
    }
}