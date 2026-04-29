package net.xiaoyu.mob_controller.rule;

import net.minecraft.resources.ResourceLocation;

public record CustomControlRule(
        ResourceLocation mobId,
        ResourceLocation itemId,
        double chance,
        int requiredHealth,
        boolean consumeItem
) {
    public static CustomControlRule parse(String line) {
        String[] parts = line.split(";");
        if (parts.length != 5) {
            throw new IllegalArgumentException("Invalid custom control rule (need 5 parts): " + line);
        }
        try {
            return new CustomControlRule(
                    new ResourceLocation(parts[0]),
                    new ResourceLocation(parts[1]),
                    Double.parseDouble(parts[2]),
                    Integer.parseInt(parts[3]),
                    Boolean.parseBoolean(parts[4])
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse rule: " + line, e);
        }
    }
}