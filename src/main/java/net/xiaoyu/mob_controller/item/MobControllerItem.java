package net.xiaoyu.mob_controller.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.entity.EntityControlledPillager;
import net.xiaoyu.mob_controller.entity.EntityControlledWitch;
import net.xiaoyu.mob_controller.registry.ModEntities;
import net.xiaoyu.mob_controller.util.MobControlledData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

public class MobControllerItem extends Item {
    public static final Map<EntityType<?>, Function<Entity, Mob>> ENTITY_TYPE_FUNCTION_MAP = new HashMap<>();

    public MobControllerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof Mob mob) {
            Level level = player.level();

            if (!level.isClientSide) {
                /*if (MobControlledData.isControlledMob(mob) && MobControlledData.getControllerUUID(mob).equals(player.getUUID())) {
                    if (player.isShiftKeyDown()) {
                        MobControlledData.ControlMode newMode = MobControlledData.toggleControlMode(mob);
                        String mobName = mob.getDisplayName().getString();

                        String modeKey = (newMode == MobControlledData.ControlMode.FOLLOW) ? "mob_controller.mode.follow" : "mob_controller.mode.stay";

                        MobControlUtil.showMessageToPlayer(player, mobName, modeKey, new Object[]{}, ChatFormatting.GOLD);
                        
                        return InteractionResult.SUCCESS;
                    }
                }*/

                if (MobControlledData.isControlledMob(mob)) {
                    return InteractionResult.PASS;
                }

                if (mob.getHealth() >= 10.0F) {
                    spawnParticles(mob, false);
                    return InteractionResult.FAIL;
                }

                if (MobControlledData.hasPlayerControlledSameHighHealthMob(player.getUUID(), mob)) {
                    /*MobControlUtil.showMessageToPlayer(
                        player, null, "mob_controller.error.same_high_health_mob", 
                        new Object[]{ MobControlledData.HIGH_HEALTH_THRESHOLD }, ChatFormatting.RED
                    );*/

                    spawnParticles(mob, false);
                    return InteractionResult.FAIL;
                }

                if (Config.BLACKLISTED_MOBS.get().contains(EntityType.getKey(mob.getType()).toString()) || hasOwnerOrTameTag(mob)) {
                    spawnParticles(mob, false);
                    return InteractionResult.FAIL;
                }

                float controlChance = 1.0f;

                if (!Config.ALWAYS_SUCCESS.get()) {
                    controlChance = calculateControlChance(mob);
                }

                if (level.random.nextFloat() <= controlChance) {
                    mob.setTarget(null);
                    // 控制成功
                    controlMob(player, mob);
                    spawnParticles(mob, true);
                    return InteractionResult.SUCCESS;
                } else {
                    // 控制失败
                    spawnParticles(mob, false);
                    return InteractionResult.FAIL;
                }
            }
        }
        return InteractionResult.PASS;
    }

    private boolean hasOwnerOrTameTag(Mob mob) {
        if (mob instanceof TamableAnimal tamable) {
            if (tamable.isTame()) {
                return true;
            }
        }

        CompoundTag nbt = mob.saveWithoutId(new CompoundTag());
        if (nbt.contains("Owner") || nbt.contains("OwnerUUID")) {
            return true;
        }
        if (nbt.contains("Tame") && nbt.getBoolean("Tame")) {
            return true;
        }
        return mob instanceof TamableAnimal;
    }

    private float calculateControlChance(Mob mob) {
        if (mob instanceof TamableAnimal tamable) {
            if (tamable.isTame()) {
                return 0.0f;
            }
        }

        float maxHealth = mob.getMaxHealth();

        if (maxHealth < 10) {
            return 1.0f;
        } else if (maxHealth <= 50) {
            return 1.0f;
        } else {
            float extraHealth = maxHealth - 50;
            int segments = (int) (extraHealth / 50);
            float reduction = segments * 0.2f;
            float chance = 1.0f - reduction;
            return Math.max(chance, 0.2f);
        }
    }

    private void controlMob(Player player, Mob mob) {
        MobControlledData.addControlledMob(player.getUUID(), mob);
        // 消除被控制的生物仇恨(32格内)
        if (!mob.level().isClientSide && mob.level() instanceof ServerLevel serverLevel) {
            for (Entity entity : mob.level().getEntitiesOfClass(Entity.class, mob.getBoundingBox().inflate(32.0))) {
                if (entity instanceof Mob oldMob && MobControlledData.isControlledMob(oldMob)) {
//                    if (oldMob instanceof Guardian) {
                    Mob newMob = getNewMob(oldMob, serverLevel);
                    serverLevel.addFreshEntity(newMob);
//                    } else {
//                        mob.setTarget(null);
//                        MobControlledData.clearSystemAttack(oldMob);
//                    }
                }
            }
        }
    }

    private static Mob getNewMob(Mob oldMob, ServerLevel serverLevel) {
        AtomicReference<Mob> atomicNewMob = new AtomicReference<>();
        ENTITY_TYPE_FUNCTION_MAP.forEach((entityType, entityFunction) -> {
            if (oldMob.getType().equals(entityType)) {
                atomicNewMob.set(entityFunction.apply(oldMob));
            }
        });
        if (atomicNewMob.get() != null) {
            return atomicNewMob.get();
        }

        CompoundTag nbt = oldMob.saveWithoutId(new CompoundTag());

        double x = oldMob.getX();
        double y = oldMob.getY();
        double z = oldMob.getZ();
        float yRot = oldMob.getYRot();
        float xRot = oldMob.getXRot();

        oldMob.remove(Entity.RemovalReason.DISCARDED);

        Mob newMob = oldGetNewMob(oldMob, serverLevel);

        newMob.load(nbt);

        newMob.setPos(x, y, z);
        newMob.setYRot(yRot);
        newMob.setXRot(xRot);
        return newMob;
    }

    private static Mob oldGetNewMob(Mob oldMob, ServerLevel serverLevel) {
        Mob newMob;
        if (oldMob instanceof ElderGuardian) {
            // 远古守卫者
            newMob = new ElderGuardian(EntityType.ELDER_GUARDIAN, serverLevel);
        } else if (oldMob instanceof Hoglin) {
            // 疣猪兽
            newMob = new Hoglin(EntityType.HOGLIN, serverLevel);
        } else if (oldMob instanceof Piglin) {
            // 猪灵
            newMob = new Piglin(EntityType.PIGLIN, serverLevel);
        } else if (oldMob instanceof PiglinBrute) {
            // 猪灵蛮兵
            newMob = new PiglinBrute(EntityType.PIGLIN_BRUTE, serverLevel);
        } else if (oldMob instanceof Slime) {
            // 史莱姆
            newMob = new Slime(EntityType.SLIME, serverLevel);
        } else {
            // 守卫者
            newMob = new Guardian(EntityType.GUARDIAN, serverLevel);
        }
        return newMob;
    }

    private void spawnParticles(Mob mob, boolean success) {
        Level level = mob.level();

        if (level.isClientSide) {
            return;
        }

        // 控制成功
        if (success) {
            ((ServerLevel) level).sendParticles(
                    ParticleTypes.HEART,
                    mob.getX(),
                    mob.getY() + mob.getBbHeight(),
                    mob.getZ(),
                    7,
                    0.5, 0.5, 0.5,
                    0.1
            );
        }
        // 控制失败
        else {
            ((ServerLevel) level).sendParticles(
                    ParticleTypes.ANGRY_VILLAGER,
                    mob.getX(),
                    mob.getY() + mob.getBbHeight(),
                    mob.getZ(),
                    7,
                    0.5, 0.5, 0.5,
                    0.1
            );
        }
    }

    private static <T extends Entity> Mob newMob(Entity oldEntity, EntityType<T> entityType, BiFunction<EntityType<T>, ServerLevel, Mob> newMobFunction) {
        CompoundTag nbt = oldEntity.saveWithoutId(new CompoundTag());

        double x = oldEntity.getX();
        double y = oldEntity.getY();
        double z = oldEntity.getZ();
        float yRot = oldEntity.getYRot();
        float xRot = oldEntity.getXRot();

        ServerLevel serverLevel = (ServerLevel) oldEntity.level();

        oldEntity.remove(Entity.RemovalReason.DISCARDED);

        Mob newMob = newMobFunction.apply(entityType, serverLevel);

        newMob.load(nbt);

        newMob.setPos(x, y, z);
        newMob.setYRot(yRot);
        newMob.setXRot(xRot);

        return newMob;
    }

    static {
        ENTITY_TYPE_FUNCTION_MAP.put(EntityType.PILLAGER, oldEntity ->
                newMob(oldEntity, ModEntities.CONTROLLED_PILLAGER.get(), EntityControlledPillager::new));
        ENTITY_TYPE_FUNCTION_MAP.put(EntityType.WITCH, oldEntity ->
                newMob(oldEntity, ModEntities.CONTROLLED_WITCH.get(), EntityControlledWitch::new));
    }
}