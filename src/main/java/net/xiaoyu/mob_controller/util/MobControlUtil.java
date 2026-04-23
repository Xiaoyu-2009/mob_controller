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
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.warden.AngerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.mixin.AccessorSlimeMoveControl;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
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
        return mob.getType().equals(EntityType.TURTLE)
            || mob.getType().equals(EntityType.FROG);
    }
    
    /**
     * 在每刻中处理被控制生物的跟随逻辑。
     *
     * <p>包含不同生物类型的差异化移动控制，以及距离过远时的安全传送。</p>
     *
     * @param mob 被控制生物
     */
    public static void handleMobFollowing(Mob mob) {
        
        if (MobControlledData.isControlledEntity(mob)) {
            Player controller = MobControlledData.getController(mob, mob.level());
            
            if (controller != null && !controller.isSpectator()) {
                double distanceSq = controller.distanceToSqr(mob);
                
                // 跟随
                if (distanceSq > 64.0D) { // 8格距离
                    if (mob instanceof Ghast || mob instanceof Vex || mob instanceof Blaze) {
                        // 恶魂/恼鬼/烈焰人
                        mob.getMoveControl().setWantedPosition(controller.getX(), controller.getY() + 2.0D, controller.getZ(), 1.0D);
                    }/*  else if (mob instanceof WitherBoss) {
                        // 凋零
                        WitherBoss wither = (WitherBoss) mob;
                        wither.getMoveControl().setWantedPosition(controller.getX(), controller.getY() + 2.0D, controller.getZ(), 1.0D);
                    } */ else if (mob instanceof Phantom phantom) {
                        // 幻翼
                        phantom.setTarget(controller);
                        
                        try {
                            Field attackPhaseField = Phantom.class.getDeclaredField("attackPhase");
                            attackPhaseField.setAccessible(true);
                            
                            Class<?> attackPhaseClass = Class.forName("net.minecraft.world.entity.monster.Phantom$AttackPhase");
                            Object[] attackPhaseConstants = attackPhaseClass.getEnumConstants();
                            
                            for (Object constant : attackPhaseConstants) {
                                if ("SWOOP".equals(constant.toString())) {
                                    attackPhaseField.set(phantom, constant);
                                    break;
                                }
                            }
                        } catch (Exception ignored) {
                        }
                        
                        try {
                            Field moveTargetPointField = Phantom.class.getDeclaredField("moveTargetPoint");
                            moveTargetPointField.setAccessible(true);
                            
                            moveTargetPointField.set(phantom, new Vec3(controller.getX(), controller.getY() + 1.0D, controller.getZ()));
                        } catch (Exception ignored) {
                        }
                    } else if (mob instanceof Squid squid) {
                        // 鱿鱼
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
                        // 蝙蝠
                        
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
                    }/*  else if (mob instanceof Bee) {
                        // 蜜蜂
                        Bee bee = (Bee) mob;

                        try {
                            Method setHasNectarMethod = Bee.class.getDeclaredMethod("setHasNectar", boolean.class);

                            setHasNectarMethod.setAccessible(true);
                            setHasNectarMethod.invoke(bee, false);
                        } catch (Exception e) {}
                    } */ else {
                        // 一般的生物...
                        mob.getNavigation().moveTo(controller, 1.0D);
                        if (mob.getMoveControl() instanceof AccessorSlimeMoveControl slimeMoveControl) {
                            slimeMoveControl.mob_controller$setDirection(getYawTowards(mob, controller), true);
                        }
                    }
                    
                    // 传送
// 传送
                    if (distanceSq > 196.0D && mob.getVehicle() == null) {
                        BlockPos controllerPos = controller.blockPosition();
                        
                        // 是否要传送到水中：原逻辑根据生物的水生类型判定，两栖动物根据控制者是否在水中动态判定
                        boolean needsWaterTeleport;
                        if (isAmphibian(mob)) {
                            // 两栖动物：如果控制者完全浸没在水中，则传送到水中；否则传送到陆地
                            needsWaterTeleport = isControllerFullySubmerged(controller);
                        } else {
                            needsWaterTeleport = mob.getMobType().equals(MobType.WATER);
                        }
                        
                        if (needsWaterTeleport) {
                            // 控制者是否在水中
                            if (isControllerFullySubmerged(controller)) {
                                BlockPos safeWaterPos = findSafePosition(mob, controller, true);
                                if (safeWaterPos != null) {
                                    teleportMob(mob, safeWaterPos);
                                }
                            }
                        } else {
                            // 控制者下方3格为非流体
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
                    }
                }
            }
        }
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
        // 控制者头部/身体是否完全在水中
        return controller.isInWater() &&
            controller.level().getFluidState(controller.blockPosition()).getType().equals(Fluids.WATER) &&
            controller.level().getFluidState(controller.blockPosition().above()).getType().equals(Fluids.WATER);
    }
    
    @Nullable
    private static BlockPos findSafePosition(Mob mob, Player controller, boolean isWater) {
        AABB mobAABB = mob.getBoundingBox();
        BlockPos controllerPos = controller.blockPosition();
        
        // 7x7x7范围内
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos checkPos = controllerPos.offset(x, y, z);
                    
                    if (isWater) {
                        // 是否是水
                        if (mob.level().getFluidState(checkPos).getType().equals(Fluids.WATER)) {
                            // 上方是否也是水
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
                        // 是否是空气
                        if (mob.level().getBlockState(checkPos).isAir()) {
                            // 下方是否有可站立的方块
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
        
        // 目标是否是控制者
        if (target.equals(controller)) {
            return false;
        }
        
        // 受控生物默认不主动敌对玩家，玩家仅能走防御反击链路。
        if (target instanceof Player) {
            return false;
        }
        
        // 目标是否是控制者的宠物
        if (target instanceof OwnableEntity ownable) {
            if (controllerUUID != null) {
                LivingEntity owner = ownable.getOwner();
                return owner == null || !owner.getUUID().equals(controllerUUID);
            }
        }
        
        // 目标是否有自定义名称且与控制者名称相同
        /*if (controllerUUID != null) {
            Player targetController = MobControlledData.getController(controlledMob, controlledMob.level());

            if (targetController != null) {
                Component controllerName = targetController.getName();
                Component targetName = target.getName();

                if (targetName != null && controllerName != null) {
                    if (targetName.getString().equals(controllerName.getString())) {
                        return false;
                    }
                }
            }
        }*/
        
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
            || (
            controlledMob instanceof Mob mob
                && MobControlledData.isSystemAttack(mob)
                && (
                canRetaliateAgainst(controlledMob, target)
                    || canAttackPlayerByOwnerCommand(controlledMob, target)
            )
        );
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
}
