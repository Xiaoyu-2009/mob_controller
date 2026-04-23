package net.xiaoyu.mob_controller.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.entity.EntityControlledPillager;
import net.xiaoyu.mob_controller.entity.EntityControlledWitch;
import net.xiaoyu.mob_controller.registry.ModEntities;
import net.xiaoyu.mob_controller.util.MobControlUtil;
import net.xiaoyu.mob_controller.util.MobControlledData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 生物控制器物品。
 *
 * <p>用于尝试控制目标生物，并提供“控制令”批量切换模式的服务端逻辑支持。</p>
 */
public class MobControllerItem extends Item {
    /**
     * 特殊生物类型替换函数表（如灾厄村民变体）。
     */
    public static final Map<EntityType<?>, Function<Entity, Mob>> ENTITY_TYPE_FUNCTION_MAP = new HashMap<>();
    /**
     * 控制令生效半径（以方块为单位）。
     */
    private static final int CONTROL_COMMAND_RANGE = 32;
    /**
     * 控制令生效后给予发光效果的持续时长。
     */
    private static final int GLOWING_DURATION_TICKS = 100;
    
    static {
        ENTITY_TYPE_FUNCTION_MAP.put(
            EntityType.PILLAGER, oldEntity ->
                newMob(oldEntity, ModEntities.CONTROLLED_PILLAGER.get(), EntityControlledPillager::new)
        );
        ENTITY_TYPE_FUNCTION_MAP.put(
            EntityType.WITCH, oldEntity ->
                newMob(oldEntity, ModEntities.CONTROLLED_WITCH.get(), EntityControlledWitch::new)
        );
    }
    
    /**
     * 构造生物控制器物品。
     *
     * @param properties 物品属性
     */
    public MobControllerItem(Properties properties) {
        super(properties);
    }
    
    /**
     * 对玩家周围所有受其控制的生物批量应用控制模式。
     *
     * @param player 执行者玩家
     * @param mode   目标控制模式
     * @return 受影响生物数量
     */
    public static int applyControlCommand(Player player, MobControlledData.ControlMode mode) {
        if (player.level().isClientSide) {
            return 0;
        }
        
        AABB area = player.getBoundingBox().inflate(CONTROL_COMMAND_RANGE);
        List<Mob> controlledMobs = player.level().getEntitiesOfClass(
            Mob.class, area, mob ->
                MobControlledData.isControlledEntity(mob) && player.getUUID().equals(MobControlledData.getControllerUUID(mob))
        );
        
        for (Mob mob : controlledMobs) {
            MobControlledData.setControlMode(mob, mode);
            mob.setTarget(null);
            MobControlledData.clearSystemAttack(mob);
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOWING_DURATION_TICKS));
        }
        
        return controlledMobs.size();
    }
    
    /**
     * 使用旧实体 NBT 创建新实体实例，并尽量保持位置和朝向。
     *
     * @param oldEntity      原实体
     * @param entityType     新实体类型
     * @param newMobFunction 新实体构造函数
     * @param <T>            实体泛型
     * @return 新创建的生物实体
     */
    private static <T extends Entity> Mob newMob(
        Entity oldEntity,
        EntityType<T> entityType,
        BiFunction<EntityType<T>, ServerLevel, Mob> newMobFunction
    ) {
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
    
    /**
     * 玩家对生物右键时尝试执行控制。
     *
     * @param stack  手持物品堆
     * @param player 操作玩家
     * @param target 目标实体
     * @param hand   交互手
     * @return 交互结果
     */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof Mob mob) {
            Level level = player.level();
            
            if (!level.isClientSide) {
                if (MobControlledData.isControlledEntity(mob)) {
                    return InteractionResult.PASS;
                }
                
                if (!Config.ALWAYS_SUCCESS.get() && mob.getHealth() > 10.0F) {
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
                    MobControlUtil.showMessageToPlayer(player, mob.getDisplayName(), "mob_controller.mode.follow", new Object[]{}, ChatFormatting.GOLD);
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
    
    /**
     * 检查生物是否属于已驯服或已有主人的实体。
     *
     * @param mob 目标生物
     * @return {@code true} 表示不允许被该物品控制
     */
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
    
    /**
     * 根据生物最大生命值计算控制成功率。
     *
     * @param mob 目标生物
     * @return 0.0~1.0 之间的成功概率
     */
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
    
    /**
     * 将目标生物标记为被指定玩家控制，并清理附近受控生物仇恨。
     *
     * @param player 控制者玩家
     * @param mob    目标生物
     */
    private void controlMob(Player player, Mob mob) {
        MobControlledData.addControlledMob(player.getUUID(), mob);
        // 消除被控制的生物仇恨(32格内)
        if (!mob.level().isClientSide && mob.level() instanceof ServerLevel serverLevel) {
            for (Entity entity : mob.level().getEntitiesOfClass(Entity.class, mob.getBoundingBox().inflate(32.0))) {
                if (entity instanceof Mob oldMob && MobControlledData.isControlledEntity(oldMob)) {
                    if (oldMob.getTarget() != null && oldMob.getTarget().is(mob)) {
                        oldMob.setTarget(null);
                    }
                    if (mob.getTarget() != null && mob.getTarget().is(oldMob)) {
                        mob.setTarget(null);
                    }
                    AtomicReference<Mob> atomicNewMob = new AtomicReference<>();
                    ENTITY_TYPE_FUNCTION_MAP.forEach((entityType, entityFunction) -> {
                        if (oldMob.getType().equals(entityType)) {
                            atomicNewMob.set(entityFunction.apply(oldMob));
                        }
                    });
                    if (atomicNewMob.get() != null) {
                        serverLevel.addFreshEntity(atomicNewMob.get());
                    } else {
                        mob.setTarget(null);
                        MobControlledData.clearSystemAttack(oldMob);
                    }
                }
            }
            if (mob.getVehicle() instanceof Mob vehicle) {
                controlMob(player, vehicle);
            }
        }
    }
    
    /**
     * 在服务端生成控制成功/失败粒子效果。
     *
     * @param mob     目标生物
     * @param success 是否控制成功
     */
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
}
