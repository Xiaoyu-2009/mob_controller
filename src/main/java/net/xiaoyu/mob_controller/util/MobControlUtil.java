package net.xiaoyu.mob_controller.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.warden.AngerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.mixin.AccessorSlimeMoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.Zoglin;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 生物控制系统的通用工具类。
 *
 * <p>主要提供被控制生物的跟随/停留行为处理、敌友判定、目标设置，
 * 以及向玩家发送动作栏与标题提示等能力。</p>
 */
public class MobControlUtil {
    /**
     * 停留模式坐标焊死数据的持久化键。
     */
    private static final String STAY_WELD_TAG = "mob_controller:stay_weld";
    private static final String STAY_WELD_X = "x";
    private static final String STAY_WELD_Y = "y";
    private static final String STAY_WELD_Z = "z";

    /**
     * 判断生物是否为两栖动物（海龟或青蛙）。
     * 两栖动物在传送时会根据控制者的位置智能选择水中或陆地传送点。
     */
    private static boolean isAmphibian(Mob mob) {
        List<? extends String> amphibianList = Config.AMPHIBIAN_MOBS.get();
        String entityId = EntityType.getKey(mob.getType()).toString();
        return amphibianList.contains(entityId);
    }

    // ========== 新增：传送落点判定核心方法 ==========
    /**
     * 判断指定生物在跟随传送时应当被传送到水中还是陆地。
     *
     * @param mob        受控生物
     * @param controller 控制者玩家
     * @return true – 传送到水中；false – 传送到陆地（空气方块）
     */
    private static boolean shouldTeleportToWater(Mob mob, Player controller) {
        String entityId = EntityType.getKey(mob.getType()).toString();

        // 1. 两栖生物特殊处理（原本逻辑）
        if (Config.AMPHIBIAN_MOBS.get().contains(entityId)) {
            return isControllerFullySubmerged(controller);
        }

        // 2. 强制陆生列表（最高优先级覆盖）
        if (Config.LAND_BASED_MOBS.get().contains(entityId)) {
            return false;
        }

        // 3. 强制水生列表
        if (Config.WATER_BASED_MOBS.get().contains(entityId)) {
            return true;
        }

        // 4. 回退到原版类型判定
        return mob.getMobType().equals(MobType.WATER);
    }

    /**
     * 在每刻中处理被控制生物的跟随逻辑。
     * <p>如果生物有有效的战斗目标（即正处于战斗中），则只执行距离过远时的传送，
     * 不执行主动移动控制，避免干扰战斗行为。</p>
     * <p>水平传送距离、垂直传送距离、跟随移动距离均可通过配置文件调整。</p>
     *
     * @param mob 被控制生物
     */
    public static void handleMobFollowing(Mob mob) {
        if (!MobControlledData.isControlledEntity(mob)) {
            return;
        }
        Player controller = MobControlledData.getController(mob, mob.level());
        if (controller == null || controller.isSpectator()) {
            return;
        }

        double distanceSq = controller.distanceToSqr(mob);
        boolean hasCombat = hasCombatTarget(mob);   // 使用统一的战斗目标判定

        // 读取跟随移动触发距离（从配置文件）
        double moveToDist = Config.FOLLOW_MOVE_TO_DISTANCE.get();
        double moveToDistSq = moveToDist * moveToDist;

        // 有战斗目标时，跳过移动控制（仅保留传送）
        if (!hasCombat && distanceSq > moveToDistSq) {
            // 跟随移动逻辑（无战斗目标时执行）
            if (mob instanceof Ghast || mob instanceof Vex || mob instanceof Blaze) {
                mob.getMoveControl().setWantedPosition(controller.getX(), controller.getY() + 2.0D, controller.getZ(), 1.0D);
            } else if (mob instanceof Phantom phantom) {
                // 跟随模式下，直接让幻翼飞向控制器
                phantom.getMoveControl().setWantedPosition(controller.getX(), controller.getY() + 2.0D, controller.getZ(), 1.0D);
            } else if (mob instanceof Squid squid) {
                Vec3 direction = new Vec3(
                        controller.getX() - mob.getX(),
                        controller.getY() - mob.getY(),
                        controller.getZ() - mob.getZ()
                ).normalize();
                squid.setMovementVector(
                        (float) (direction.x * 0.2F),
                        (float) (direction.y * 0.2F),
                        (float) (direction.z * 0.2F)
                );
            } else if (mob instanceof Bat bat) {
                if (bat.isResting()) {
                    bat.setResting(false);
                }
                try {
                    Field targetPositionField = Bat.class.getDeclaredField("targetPosition");
                    targetPositionField.setAccessible(true);
                    targetPositionField.set(
                            bat, new BlockPos(
                                    (int) controller.getX(),
                                    (int) controller.getY() + 2,
                                    (int) controller.getZ()
                            )
                    );
                } catch (Exception ignored) {
                }
            } else {
                mob.getNavigation().moveTo(controller, 1.0D);
                if (mob.getMoveControl() instanceof AccessorSlimeMoveControl slimeMoveControl) {
                    slimeMoveControl.mob_controller$setDirection(getYawTowards(mob, controller), true);
                }
            }
        }

        // 传送逻辑：水平距离、垂直距离超过配置阈值时触发
        double teleportHorizDist = Config.FOLLOW_TELEPORT_HORIZONTAL_DISTANCE.get();
        double teleportVertDist = Config.FOLLOW_TELEPORT_VERTICAL_DISTANCE.get();
        double dx = mob.getX() - controller.getX();
        double dz = mob.getZ() - controller.getZ();
        double horizontalDistSq = dx * dx + dz * dz;
        double verticalDist = Math.abs(mob.getY() - controller.getY());
        boolean shouldTeleport = horizontalDistSq > teleportHorizDist * teleportHorizDist || verticalDist > teleportVertDist;

        if (shouldTeleport) {
            if (mob.getVehicle() == null) {
                // 无骑乘物：生物自身传送到玩家身边
                BlockPos controllerPos = controller.blockPosition();
                boolean needsWaterTeleport = shouldTeleportToWater(mob, controller);

                if (needsWaterTeleport) {
                    if (isControllerFullySubmerged(controller)) {
                        BlockPos safeWaterPos = findSafePosition(mob, controller, true);
                        if (safeWaterPos != null) {
                            teleportMob(mob, safeWaterPos);
                        }
                    }
                } else {
                    boolean nonFluidBlockFound = false;
                    for (int i = 0; i < 3; i++) {
                        BlockPos checkPos = controllerPos.below(i + 1);
                        BlockState state = mob.level().getBlockState(checkPos);
                        if (state.getFluidState().getType().equals(Fluids.EMPTY)) {
                            nonFluidBlockFound = true;
                            break;
                        }
                    }
                    if (nonFluidBlockFound) {
                        BlockPos safePos = findSafePosition(mob, controller, false);
                        if (safePos != null) {
                            teleportMob(mob, safePos);
                        }
                    }
                }
            } else {
                // 有骑乘物（骑兵或叠罗汉）：将整条骑乘链的根坐骑传送到玩家身边
                Entity rootVehicle = mob.getRootVehicle();
                if (rootVehicle instanceof Mob rideMob) {
                    boolean needsWaterTeleport = shouldTeleportToWater(rideMob, controller);
                    BlockPos safePos = findSafePosition(rideMob, controller, needsWaterTeleport);
                    if (safePos != null) {
                        teleportMob(rideMob, safePos);
                    }
                }
            }
        }
    }

    /**
     * 判断生物当前是否存在有效的战斗目标。
     * <p>有效目标定义为：存活、同一维度、距离 ≤ 64 格，且未被标记为不可攻击。</p>
     *
     * @param mob 生物
     * @return true 表示有可攻击的战斗目标
     */
    public static boolean hasCombatTarget(Mob mob) {
        LivingEntity target = mob.getTarget();
        if (isValidCombatTarget(mob, target)) {
            return true;
        }
        // 对 Brain 类生物额外检查内存中的攻击目标（如猪灵、疣猪兽等）
        if (mob instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin
                || mob instanceof net.minecraft.world.entity.monster.hoglin.Hoglin
                || mob instanceof net.minecraft.world.entity.monster.Zoglin) {
            java.util.Optional<LivingEntity> brainTarget = mob.getBrain().getMemory(
                    net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET);
            if (brainTarget.isPresent() && isValidCombatTarget(mob, brainTarget.get())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断给定的目标是否为当前生物的合法战斗目标。
     *
     * @param mob    攻击者
     * @param target 候选目标
     * @return true 表示目标有效且可战斗
     */
    private static boolean isValidCombatTarget(Mob mob, @Nullable LivingEntity target) {
        return target != null
                && target.isAlive()
                && !target.isDeadOrDying()
                && target.level().equals(mob.level())
                && target.distanceToSqr(mob) <= 64.0D * 64.0D;
    }

    private static float getYawTowards(Entity source, Entity target) {
        double dx = target.getX() - source.getX();
        double dz = target.getZ() - source.getZ();
        return (float) (Mth.atan2(dz, dx) * (180.0F / (float) Math.PI)) - 90.0F;
    }

    /**
     * 判断当前生物在“停留”模式下是否需要应用飞行坐标焊死。
     *
     * @param mob 生物实体
     * @return 若命中配置白名单则返回 {@code true}
     */
    public static boolean shouldUseStayFlightWeld(Mob mob) {
        String entityId = EntityType.getKey(mob.getType()).toString();
        return Config.STAY_WELDED_SPECIAL_AI_MOBS.get().contains(entityId);
    }

    /**
     * 判断受控生物是否属于当前支持直接骑乘的类型。
     */
    public static boolean isDirectRideableControlledMob(Mob mob) {
        return mob instanceof Guardian
                || mob instanceof Hoglin
                || mob instanceof Zoglin
                || mob instanceof Ravager
                || mob instanceof Cow
                || mob instanceof Sheep
                || mob instanceof Dolphin
                || mob instanceof Panda
                || mob instanceof PolarBear
                || mob instanceof Goat
                || mob.getType().equals(EntityType.SNIFFER);
    }

    /**
     * 对飞行/特殊 AI 生物应用停留坐标焊死。
     *
     * <p>首次调用会记录当前位置，后续每次强制瞬移回记录坐标并清空速度。</p>
     *
     * @param mob 生物实体
     */
    public static void applyStayFlightCoordinateWeld(Mob mob) {
        if (mob.getVehicle() != null) {
            return;
        }

        CompoundTag persistentData = mob.getPersistentData();
        CompoundTag stayWeldData;
        if (persistentData.contains(STAY_WELD_TAG, CompoundTag.TAG_COMPOUND)) {
            stayWeldData = persistentData.getCompound(STAY_WELD_TAG);
        } else {
            stayWeldData = new CompoundTag();
            stayWeldData.putDouble(STAY_WELD_X, mob.getX());
            stayWeldData.putDouble(STAY_WELD_Y, mob.getY());
            stayWeldData.putDouble(STAY_WELD_Z, mob.getZ());
            persistentData.put(STAY_WELD_TAG, stayWeldData);
        }

        mob.setDeltaMovement(Vec3.ZERO);
        mob.hasImpulse = true;
        mob.fallDistance = 0;
        mob.teleportTo(stayWeldData.getDouble(STAY_WELD_X), stayWeldData.getDouble(STAY_WELD_Y), stayWeldData.getDouble(STAY_WELD_Z));
    }

    /**
     * 清除生物的停留坐标焊死数据。
     *
     * @param mob 生物实体
     */
    public static void clearStayFlightCoordinateWeld(Mob mob) {
        mob.getPersistentData().remove(STAY_WELD_TAG);
    }

    private static void teleportMob(Mob mob, BlockPos pos) {
        mob.teleportTo(pos.getX(), pos.getY(), pos.getZ());
        mob.getNavigation().stop();
        mob.getNavigation().createPath(mob.blockPosition(), 10);
    }

    private static boolean isControllerFullySubmerged(Player controller) {
        return controller.isInWater() &&
                controller.level().getFluidState(controller.blockPosition()).getType().equals(Fluids.WATER) &&
                controller.level().getFluidState(controller.blockPosition().above()).getType().equals(Fluids.WATER);
    }

    @Nullable
    private static BlockPos findSafePosition(Mob mob, Player controller, boolean isWater) {
        AABB mobAABB = mob.getBoundingBox();
        BlockPos controllerPos = controller.blockPosition();

        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos checkPos = controllerPos.offset(x, y, z);

                    if (isWater) {
                        if (mob.level().getFluidState(checkPos).getType().equals(Fluids.WATER)) {
                            BlockPos upperPos = checkPos.above();
                            if (mob.level().getFluidState(upperPos).getType().equals(Fluids.WATER)) {
                                AABB targetAABB = mobAABB.move(
                                        checkPos.getX() - mobAABB.minX,
                                        checkPos.getY() - mobAABB.minY,
                                        checkPos.getZ() - mobAABB.minZ
                                );
                                if (mob.level().noCollision(mob, targetAABB)) {
                                    return checkPos;
                                }
                            }
                        }
                    } else {
                        if (mob.level().getBlockState(checkPos).isAir()) {
                            BlockPos groundPos = checkPos.below();
                            BlockState groundState = mob.level().getBlockState(groundPos);
                            if (groundState.isFaceSturdy(mob.level(), groundPos, Direction.UP)) {
                                AABB targetAABB = mobAABB.move(
                                        checkPos.getX() - mobAABB.minX,
                                        checkPos.getY() - mobAABB.minY,
                                        checkPos.getZ() - mobAABB.minZ
                                );
                                if (mob.level().noCollision(mob, targetAABB)) {
                                    return checkPos;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 判定目标是否应被视为被控制生物的敌对对象。
     *
     * @param controlledMob 被控制生物
     * @param target        目标实体，可为 {@code null}
     * @return {@code true} 表示可视为敌对目标
     */
    public static boolean isEnemy(LivingEntity controlledMob, @Nullable Entity target) {
        if (target == null) {
            return false;
        }
        if (!MobControlledData.isControlledEntity(controlledMob)) {
            return false;
        }
        if (target instanceof LivingEntity mob && MobControlledData.isControlledEntity(mob)
                && Objects.equals(MobControlledData.getControllerUUID(controlledMob), MobControlledData.getControllerUUID(mob))) {
            return false;
        }

        Player controller = MobControlledData.getController(controlledMob, controlledMob.level());
        UUID controllerUUID = MobControlledData.getControllerUUID(controlledMob);

        if (target.equals(controller)) {
            return false;
        }

        if (target instanceof Player) {
            return false;
        }

        if (target instanceof OwnableEntity ownable) {
            if (controllerUUID != null) {
                LivingEntity owner = ownable.getOwner();
                return owner == null || !owner.getUUID().equals(controllerUUID);
            }
        }

        return true;
    }

    /**
     * 判定目标是否可作为受控生物的“防御反击”对象。
     */
    public static boolean canRetaliateAgainst(LivingEntity controlledMob, @Nullable LivingEntity target) {
        if (target == null) {
            return false;
        }
        if (isEnemy(controlledMob, target)) {
            return true;
        }
        if (!(target instanceof Player player)) {
            return false;
        }
        if (!MobControlledData.isControlledEntity(controlledMob)) {
            return false;
        }

        UUID controllerUUID = MobControlledData.getControllerUUID(controlledMob);
        if (controllerUUID == null
                || controllerUUID.equals(player.getUUID())
                || player.isCreative()
                || player.isSpectator()) {
            return false;
        }

        if (player.equals(controlledMob.getLastHurtByMob())) {
            return true;
        }

        Player controller = MobControlledData.getController(controlledMob, controlledMob.level());
        return controller != null && player.equals(controller.getLastHurtByMob());
    }

    /**
     * 用于攻击事件上下文：攻击者已知时允许立即进入反击。
     */
    public static boolean canRetaliateAgainstImmediateAttacker(LivingEntity controlledMob, @Nullable LivingEntity attacker) {
        if (!(attacker instanceof Player player)) {
            return canRetaliateAgainst(controlledMob, attacker);
        }
        if (!MobControlledData.isControlledEntity(controlledMob)) {
            return false;
        }

        UUID controllerUUID = MobControlledData.getControllerUUID(controlledMob);
        return controllerUUID != null
                && !controllerUUID.equals(player.getUUID())
                && !player.isCreative()
                && !player.isSpectator();
    }

    /**
     * 判定目标玩家是否可作为“主人指令攻击”的合法对象。
     *
     * <p>该逻辑仅用于主人主动攻击某玩家后，受控生物是否允许协同攻击的场景，
     * 与护主/反击逻辑相互独立。</p>
     */
    public static boolean canAttackPlayerByOwnerCommand(LivingEntity controlledMob, @Nullable LivingEntity target) {
        if (!(target instanceof Player player)) {
            return false;
        }
        if (!MobControlledData.isControlledEntity(controlledMob)) {
            return false;
        }
        if (!Config.CONTROLLED_MOBS_ATTACK_PLAYERS_ON_COMMAND.get()) {
            return false;
        }

        UUID controllerUUID = MobControlledData.getControllerUUID(controlledMob);
        return controllerUUID != null
                && !controllerUUID.equals(player.getUUID())
                && !player.isCreative()
                && !player.isSpectator();
    }

    /**
     * 判断目标实体是否为受控生物的控制者本人。
     *
     * @param controlledMob 受控生物
     * @param target        候选目标，可为 {@code null}
     * @return {@code true} 表示目标即为控制者
     */
    public static boolean isController(LivingEntity controlledMob, @Nullable Entity target) {
        if (target == null || !MobControlledData.isControlledEntity(controlledMob)) {
            return false;
        }
        UUID controllerUUID = MobControlledData.getControllerUUID(controlledMob);
        return controllerUUID != null && controllerUUID.equals(target.getUUID());
    }

    /**
     * 判定目标是否允许继续作为当前战斗目标。
     */
    public static boolean canKeepCombatTarget(LivingEntity controlledMob, @Nullable LivingEntity target) {
        return isEnemy(controlledMob, target)
                || (controlledMob instanceof Mob mob
                && MobControlledData.isSystemAttack(mob)
                && (canRetaliateAgainst(controlledMob, target)
                || canAttackPlayerByOwnerCommand(controlledMob, target)));
    }

    /**
     * 设置生物攻击目标，并兼容监守者的愤怒系统。
     *
     * @param mob    发起攻击的生物
     * @param target 目标实体
     */
    public static void setMobTargetWithAnger(Mob mob, LivingEntity target) {
        if (mob instanceof Warden warden) {
            warden.increaseAngerAt(target, AngerLevel.ANGRY.getMinimumAnger() + 20, false);
            warden.setAttackTarget(target);
        } else {
            mob.setTarget(target);
        }
    }

    /**
     * 向玩家发送着色后的动作栏提示文本。
     *
     * @param player         目标玩家
     * @param prefix         前缀文本，可为空字符串
     * @param translationKey 语言键
     * @param args           格式化参数
     * @param color          文本颜色
     */
    public static void showMessageToPlayer(Player player, Component prefix, String translationKey, Object[] args, ChatFormatting color) {
        if (player instanceof ServerPlayer serverPlayer) {
            MutableComponent message;
            if (!prefix.toString().isEmpty()) {
                message = Component.translatable("mob_controller.message.connection", prefix, Component.translatable(translationKey, args));
            } else {
                message = Component.translatable(translationKey, args);
            }
            message.setStyle(Style.EMPTY.withColor(color));
            serverPlayer.sendSystemMessage(message, true);
        }
    }

    /**
     * 判断目标是否为受控生物应主动攻击的敌对目标（基于铁傀儡逻辑）。
     * <p>条件：</p>
     * <ul>
     *   <li>目标必须是 {@link net.minecraft.world.entity.monster.Enemy} 类型；</li>
     *   <li>排除苦力怕（Creeper）；</li>
     *   <li>排除其他受控生物（无论控制者是否相同）；</li>
     *   <li>排除当前受控生物本身；</li>
     *   <li>排除控制者本人。</li>
     * </ul>
     *
     * @param controlledMob 受控生物
     * @param target        候选目标
     * @return {@code true} 表示应主动攻击
     */
    public static boolean isHostileTarget(LivingEntity controlledMob, LivingEntity target) {
        if (target == null || target == controlledMob) {
            return false;
        }
        if (target instanceof Creeper) {
            return false;
        }
        if (MobControlledData.isControlledEntity(target)) {
            return false;
        }
        if (isController(controlledMob, target)) {
            return false;
        }
        return target instanceof Enemy;
    }

    /**
     * 向玩家显示“控制模式切换”标题提示。
     *
     * @param player             目标玩家
     * @param mobName            生物显示名组件
     * @param modeTranslationKey 模式翻译键
     * @param color              标题颜色
     */
    public static void showControlModeTitle(Player player, Component mobName, String modeTranslationKey, ChatFormatting color) {
        if (player instanceof ServerPlayer serverPlayer) {
            Component title = Component.translatable(
                    "mob_controller.title.control_mode",
                    mobName,
                    Component.translatable(modeTranslationKey)
            ).setStyle(Style.EMPTY.withColor(color));

            serverPlayer.connection.send(new ClientboundSetTitlesAnimationPacket(5, 30, 10));
            serverPlayer.connection.send(new ClientboundSetTitleTextPacket(title));
        }
    }

    public static void restoreRaiderTargets(Raider raider) {
        // 移除所有现有的目标选择器（避免重复）
        raider.targetSelector.getAvailableGoals().stream()
                .filter(w -> w.getGoal() instanceof HurtByTargetGoal || w.getGoal() instanceof NearestAttackableTargetGoal)
                .toList()
                .forEach(w -> raider.targetSelector.removeGoal(w.getGoal()));

        raider.goalSelector.addGoal(0, new FloatGoal(raider));
        raider.targetSelector.addGoal(1, (new HurtByTargetGoal(raider, Raider.class)).setAlertOthers());
        raider.targetSelector.addGoal(1, new HurtByTargetGoal(raider, Raider.class).setAlertOthers());
        raider.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(raider, Player.class, true));
        raider.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(raider, AbstractVillager.class, false));
        raider.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(raider, IronGolem.class, true));
        raider.goalSelector.addGoal(8, new RandomStrollGoal(raider, 0.6D));
        raider.goalSelector.addGoal(9, new LookAtPlayerGoal(raider, Player.class, 15.0F, 1.0F));
        raider.goalSelector.addGoal(10, new LookAtPlayerGoal(raider, Mob.class, 15.0F, 1.0F));

    }

    public static void restoreWitchTargets(Witch witch) {
        // 清空已有的目标选择器
        witch.targetSelector.getAvailableGoals().stream()
                .filter(w -> w.getGoal() instanceof HurtByTargetGoal ||
                        w.getGoal() instanceof NearestAttackableTargetGoal)
                .toList()
                .forEach(w -> witch.targetSelector.removeGoal(w.getGoal()));

        witch.goalSelector.addGoal(0, new FloatGoal(witch));
        witch.targetSelector.addGoal(1, (new HurtByTargetGoal(witch, Raider.class)).setAlertOthers());
        witch.targetSelector.addGoal(1, new HurtByTargetGoal(witch));
        witch.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(witch, Player.class, true));
        witch.goalSelector.addGoal(8, new RandomStrollGoal(witch, 0.6D));
        witch.goalSelector.addGoal(9, new LookAtPlayerGoal(witch, Player.class, 15.0F, 1.0F));
        witch.goalSelector.addGoal(10, new LookAtPlayerGoal(witch, Mob.class, 15.0F, 1.0F));
    }

    /**
     * 清除生物重生后残留的战斗记忆（目标和愤怒）。
     */
    public static void clearCombatMemory(Mob mob) {
        if (mob == null) return;
        mob.setTarget(null);

        Brain<?> brain = mob.getBrain();
        if (brain == null) return;

        // 猪灵、疣猪兽、僵尸疣猪兽
        if (mob instanceof AbstractPiglin || mob instanceof Hoglin || mob instanceof Zoglin) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
            brain.eraseMemory(MemoryModuleType.ANGRY_AT);
            brain.eraseMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS);
            brain.eraseMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
        }
        // 监守者
        else if (mob instanceof Warden warden) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
            brain.eraseMemory(MemoryModuleType.ROAR_TARGET);
        }
    }

    public static boolean isAlly(LivingEntity attacker, @Nullable Entity target) {
        if (!MobControlledData.isControlledEntity(attacker)) {
            return false;
        }
        if (target == null) {
            return false;
        }
        UUID controllerUUID = MobControlledData.getControllerUUID(attacker);
        if (controllerUUID == null) {
            return false;
        }

        // 情况1: 目标就是控制者本人
        if (target instanceof Player && target.getUUID().equals(controllerUUID)) {
            return true;
        }

        // 情况2: 目标是控制者的驯服宠物 (TamableAnimal)
        if (target instanceof TamableAnimal tamable && tamable.isTame()) {
            UUID ownerUUID = tamable.getOwnerUUID();
            if (ownerUUID != null && ownerUUID.equals(controllerUUID)) {
                return true;
            }
        }

        // 情况3: 目标是另一个受控生物且控制者相同
        if (target instanceof LivingEntity livingTarget && MobControlledData.isControlledEntity(livingTarget)) {
            UUID targetControllerUUID = MobControlledData.getControllerUUID(livingTarget);
            if (targetControllerUUID != null && targetControllerUUID.equals(controllerUUID)) {
                return true;
            }
        }

        return false;
    }
}