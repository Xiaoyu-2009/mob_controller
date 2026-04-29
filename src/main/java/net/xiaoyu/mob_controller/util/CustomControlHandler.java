package net.xiaoyu.mob_controller.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.rule.CustomControlRule;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class CustomControlHandler {

    private static final Map<ResourceLocation, Map<ResourceLocation, CustomControlRule>> RULE_CACHE = new HashMap<>();
    private static boolean cacheInitialized = false;

    private static void ensureCache() {
        if (cacheInitialized) return;
        RULE_CACHE.clear();
        for (String ruleStr : Config.CUSTOM_CONTROL_RULES.get()) {
            try {
                CustomControlRule rule = CustomControlRule.parse(ruleStr);
                // 禁止使用本模组的物品作为自定义规则物品（静默跳过）
                if (rule.itemId().getNamespace().equals("mob_controller")) {
                    continue;
                }
                RULE_CACHE.computeIfAbsent(rule.mobId(), k -> new HashMap<>())
                        .put(rule.itemId(), rule);
            } catch (IllegalArgumentException e) {
            }
        }
        cacheInitialized = true;
    }

    @Nullable
    public static CustomControlRule getRule(Mob mob, Item item) {
        ensureCache();
        ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (mobId == null) return null;
        Map<ResourceLocation, CustomControlRule> itemMap = RULE_CACHE.get(mobId);
        if (itemMap == null) return null;
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        return itemMap.get(itemId);
    }

    public static boolean hasCustomRule(Mob mob) {
        ensureCache();
        ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        return mobId != null && RULE_CACHE.containsKey(mobId);
    }

    public static boolean isMatchingCustomItem(Mob mob, Item item) {
        return getRule(mob, item) != null;
    }

    public static InteractionResult handleCustomControl(Player player, Mob mob, ItemStack stack, InteractionHand hand) {
        Item item = stack.getItem();
        CustomControlRule rule = getRule(mob, item);
        if (rule == null) return InteractionResult.PASS;

        Level level = player.level();
        if (level.isClientSide) {
            player.swing(hand);
            return InteractionResult.SUCCESS;
        }

        // 已被控制的生物直接放行，交给原物品交互
        if (MobControlledData.isControlledEntity(mob)) {
            return InteractionResult.PASS;
        }

        // 全局攻击力/生命上限检查
        boolean alwaysSuccess = Config.ALWAYS_SUCCESS.get();
        if (!alwaysSuccess) {
            var attackAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            double attackDamage = attackAttr != null ? attackAttr.getValue() : 0.0;
            if (attackDamage >= Config.ATTACK_LIMIT.get()) {
                spawnParticles(mob, false);
                player.displayClientMessage(Component.translatable("mob_controller.message.attack_limit").withStyle(ChatFormatting.RED), true);
                if (rule.consumeItem() && !player.isCreative()) stack.shrink(1);
                return InteractionResult.FAIL;
            }
            float maxHealth = mob.getMaxHealth();
            if (maxHealth >= Config.HEALTH_LIMIT.get()) {
                spawnParticles(mob, false);
                player.displayClientMessage(Component.translatable("mob_controller.message.health_limit").withStyle(ChatFormatting.RED), true);
                if (rule.consumeItem() && !player.isCreative()) stack.shrink(1);
                return InteractionResult.FAIL;
            }
        }

        // 血量条件
        float currentHealth = mob.getHealth();
        if (currentHealth > rule.requiredHealth()) {
            spawnParticles(mob, false);
            player.displayClientMessage(Component.translatable("mob_controller.message.health_too_high").withStyle(ChatFormatting.RED), true);
            if (rule.consumeItem() && !player.isCreative()) stack.shrink(1);
            return InteractionResult.FAIL;
        }

        // 概率判定
        float controlChance = (float) rule.chance();
        boolean success = level.random.nextFloat() <= controlChance;

        if (success) {
            mob.setTarget(null);
            MobControlledData.addControlledMob(player.getUUID(), mob);
            MobControlUtil.showMessageToPlayer(player, mob.getDisplayName(),
                    "mob_controller.mode.follow", new Object[]{}, ChatFormatting.GOLD);
            spawnParticles(mob, true);
        } else {
            spawnParticles(mob, false);
        }

        if (rule.consumeItem() && !player.isCreative()) {
            stack.shrink(1);
        }

        return success ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    private static void spawnParticles(Mob mob, boolean success) {
        if (mob.level().isClientSide) return;
        ServerLevel serverLevel = (ServerLevel) mob.level();
        if (success) {
            serverLevel.sendParticles(ParticleTypes.HEART, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(),
                    7, 0.5, 0.5, 0.5, 0.1);
        } else {
            serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(),
                    7, 0.5, 0.5, 0.5, 0.1);
        }
    }
}