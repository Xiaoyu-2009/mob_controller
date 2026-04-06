package net.xiaoyu.mob_controller.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.locale.Language;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.warden.AngerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xiaoyu.mob_controller.mixin.AccessorSlimeMoveControl;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.UUID;

public class MobControlUtil {
    public static void handleMobFollowing(Mob mob) {

        if (MobControlledData.isControlledEntity(mob)) {
            Player controller = MobControlledData.getController(mob, mob.level());

            if (controller != null && !controller.isSpectator()) {
                mob.getLookControl().setLookAt(controller, 10.0F, (float) mob.getMaxHeadXRot());

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

                            targetPositionField.set(bat, new BlockPos(
                                    (int) controller.getX(),
                                    (int) controller.getY() + 2,
                                    (int) controller.getZ()
                            ));
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
                        mob.lookAt(controller, 10.0F, 10.0F);
                        mob.getNavigation().moveTo(controller, 1.0D);
                        if (mob.getMoveControl() instanceof AccessorSlimeMoveControl slimeMoveControl) {
                            slimeMoveControl.mob_controller$setDirection(mob.getYRot(), true);
                        }
                    }

                    // 传送
                    if (distanceSq > 196.0D && mob.getVehicle() == null) {
                        BlockPos controllerPos = controller.blockPosition();

                        // 是否要传送到水中
                        boolean needsWaterTeleport = mob.getMobType().equals(MobType.WATER);

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

    private static void teleportMob(Mob mob, BlockPos pos) {
        mob.teleportTo(pos.getX(), pos.getY(), pos.getZ());
        mob.getNavigation().stop();
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

    // 坚守者一些攻击
    public static void setMobTargetWithAnger(Mob mob, LivingEntity target) {
        if (mob instanceof Warden warden) {
            warden.increaseAngerAt(target, AngerLevel.ANGRY.getMinimumAnger() + 20, false);
            warden.setAttackTarget(target);
        } else {
            mob.setTarget(target);
        }
    }

    // 一些乱七八糟的文本...
    public static void showMessageToPlayer(Player player, String prefix, String translationKey, Object[] args, ChatFormatting color) {
        if (player instanceof ServerPlayer serverPlayer) {
            String text = Language.getInstance().getOrDefault(translationKey);
            String formattedText = args.length > 0 ? String.format(text, args) : text;

            String messageText = !prefix.isEmpty() ? prefix + " " + formattedText : formattedText;

            Component message = Component.literal(messageText).setStyle(Style.EMPTY.withColor(color));

            serverPlayer.sendSystemMessage(message, true);
        }
    }

    // 显示控制状态 title（TODO#5）
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