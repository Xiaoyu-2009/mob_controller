package net.xiaoyu.mob_controller.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.xiaoyu.mob_controller.MobController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HighHealthDatabase {
    private static final Logger LOGGER = LogManager.getLogger(MobController.MOD_ID);
    private static Connection connection = null;
    private static MinecraftServer currentServer = null;

    // 缓存：playerUUID -> (mobType -> Set<mobUUID>)  只保存存活或重生中的生物UUID
    private static final Map<UUID, Map<String, Set<UUID>>> CACHE = new ConcurrentHashMap<>();

    public static void init(MinecraftServer server) {
        if (connection != null) return;
        currentServer = server;
        File dataDir = server.getWorldPath(LevelResource.ROOT)
                .resolve("data").resolve("mob_controller").toFile();
        if (!dataDir.exists()) dataDir.mkdirs();
        String dbPath = new File(dataDir, "controlled_mobs.db").getAbsolutePath();

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS controlled_mobs (" +
                        "player_uuid TEXT NOT NULL, " +
                        "mob_uuid TEXT NOT NULL, " +
                        "mob_type TEXT NOT NULL, " +
                        "mob_nbt TEXT, " +
                        "dimension TEXT NOT NULL, " +
                        "block_x INT NOT NULL, " +
                        "block_y INT NOT NULL, " +
                        "block_z INT NOT NULL, " +
                        "is_alive INTEGER NOT NULL, " +
                        "is_respawning INTEGER NOT NULL, " +
                        "controlled_time BIGINT NOT NULL, " +
                        "PRIMARY KEY (player_uuid, mob_uuid))");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_mobtype ON controlled_mobs(player_uuid, mob_type)");
            }
            loadAllToCache();
        } catch (Exception e) {
            LOGGER.error("Failed to initialize database", e);
            connection = null;
        }
    }

    private static void ensureConnection() {
        if (connection != null) return;
        if (currentServer != null) init(currentServer);
        else {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) init(server);
        }
    }

    private static void loadAllToCache() throws SQLException {
        CACHE.clear();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT player_uuid, mob_uuid, mob_type, is_alive, is_respawning FROM controlled_mobs")) {
            while (rs.next()) {
                UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
                UUID mobUUID = UUID.fromString(rs.getString("mob_uuid"));
                String mobType = rs.getString("mob_type");
                boolean isAlive = rs.getInt("is_alive") == 1;
                boolean isRespawning = rs.getInt("is_respawning") == 1;
                if (isAlive || isRespawning) {
                    CACHE.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>())
                            .computeIfAbsent(mobType, k -> ConcurrentHashMap.newKeySet())
                            .add(mobUUID);
                }
            }
        }
    }

    private static String nbtToBase64(CompoundTag nbt) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            NbtIo.writeCompressed(nbt, baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            LOGGER.error("Failed to serialize NBT", e);
            return "";
        }
    }

    private static CompoundTag base64ToNbt(String base64) {
        if (base64 == null || base64.isEmpty()) return new CompoundTag();
        try {
            byte[] data = Base64.getDecoder().decode(base64);
            return NbtIo.readCompressed(new ByteArrayInputStream(data));
        } catch (IOException e) {
            LOGGER.error("Failed to deserialize NBT", e);
            return new CompoundTag();
        }
    }

    private static String getMobTypeId(Mob mob) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        return key == null ? "unknown" : key.toString();
    }

    /**
     * 插入一条新的受控生物记录（初始状态 alive=true, respawning=false）
     */
    public static boolean insertControlledMob(UUID playerUUID, Mob mob, CompoundTag fullNbt) {
        ensureConnection();
        if (connection == null) return false;
        String mobUUID = mob.getUUID().toString();
        String mobType = getMobTypeId(mob);
        String nbtStr = nbtToBase64(fullNbt);
        String dimension = mob.level().dimension().location().toString();
        int x = mob.getBlockX(), y = mob.getBlockY(), z = mob.getBlockZ();
        long time = System.currentTimeMillis();

        try (PreparedStatement pstmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO controlled_mobs (player_uuid, mob_uuid, mob_type, mob_nbt, dimension, block_x, block_y, block_z, is_alive, is_respawning, controlled_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, 0, ?)")) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, mobUUID);
            pstmt.setString(3, mobType);
            pstmt.setString(4, nbtStr);
            pstmt.setString(5, dimension);
            pstmt.setInt(6, x);
            pstmt.setInt(7, y);
            pstmt.setInt(8, z);
            pstmt.setLong(9, time);
            pstmt.executeUpdate();

            // 更新缓存
            CACHE.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(mobType, k -> ConcurrentHashMap.newKeySet())
                    .add(mob.getUUID());
            return true;
        } catch (SQLException e) {
            LOGGER.error("Failed to insert controlled mob", e);
            return false;
        }
    }

    /**
     * 更新生物的 NBT 和位置
     */
    public static void updateMobData(UUID playerUUID, Mob mob, CompoundTag partialNbt) {
        ensureConnection();
        if (connection == null) return;
        String mobUUID = mob.getUUID().toString();
        String nbtStr = nbtToBase64(partialNbt);
        String dimension = mob.level().dimension().location().toString();
        int x = mob.getBlockX(), y = mob.getBlockY(), z = mob.getBlockZ();

        try (PreparedStatement pstmt = connection.prepareStatement(
                "UPDATE controlled_mobs SET mob_nbt = ?, dimension = ?, block_x = ?, block_y = ?, block_z = ? WHERE player_uuid = ? AND mob_uuid = ?")) {
            pstmt.setString(1, nbtStr);
            pstmt.setString(2, dimension);
            pstmt.setInt(3, x);
            pstmt.setInt(4, y);
            pstmt.setInt(5, z);
            pstmt.setString(6, playerUUID.toString());
            pstmt.setString(7, mobUUID);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Failed to update mob data", e);
        }
    }

    /**
     * 标记生物为重生等待中（死亡但占用名额）
     */
    public static void markAsRespawning(UUID playerUUID, UUID mobUUID) {
        ensureConnection();
        if (connection == null) return;
        try (PreparedStatement pstmt = connection.prepareStatement(
                "UPDATE controlled_mobs SET is_alive = 0, is_respawning = 1 WHERE player_uuid = ? AND mob_uuid = ?")) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, mobUUID.toString());
            pstmt.executeUpdate();
            // 缓存不变（因为仍然占用名额）
        } catch (SQLException e) {
            LOGGER.error("Failed to mark as respawning", e);
        }
    }

    /**
     * 重生完成：更新为新实体的 UUID、NBT、位置，并将状态设为 alive=true, respawning=false
     */
    public static void respawnCompleted(UUID playerUUID, UUID oldMobUUID, Mob newMob, CompoundTag newNbt) {
        ensureConnection();
        if (connection == null) return;
        String newMobUUID = newMob.getUUID().toString();
        String mobType = getMobTypeId(newMob);
        String nbtStr = nbtToBase64(newNbt);
        String dimension = newMob.level().dimension().location().toString();
        int x = newMob.getBlockX(), y = newMob.getBlockY(), z = newMob.getBlockZ();

        try (PreparedStatement pstmt = connection.prepareStatement(
                "UPDATE controlled_mobs SET mob_uuid = ?, mob_type = ?, mob_nbt = ?, dimension = ?, block_x = ?, block_y = ?, block_z = ?, is_alive = 1, is_respawning = 0 WHERE player_uuid = ? AND mob_uuid = ?")) {
            pstmt.setString(1, newMobUUID);
            pstmt.setString(2, mobType);
            pstmt.setString(3, nbtStr);
            pstmt.setString(4, dimension);
            pstmt.setInt(5, x);
            pstmt.setInt(6, y);
            pstmt.setInt(7, z);
            pstmt.setString(8, playerUUID.toString());
            pstmt.setString(9, oldMobUUID.toString());
            pstmt.executeUpdate();

            // 更新缓存：移除旧UUID，加入新UUID
            Map<String, Set<UUID>> playerCache = CACHE.get(playerUUID);
            if (playerCache != null) {
                Set<UUID> set = playerCache.get(mobType);
                if (set != null) {
                    set.remove(oldMobUUID);
                    set.add(newMob.getUUID());
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to complete respawn", e);
        }
    }

    /**
     * 永久删除记录（如心变契约释放控制）
     */
    public static void deleteRecord(UUID playerUUID, UUID mobUUID) {
        ensureConnection();
        if (connection == null) return;
        try (PreparedStatement pstmt = connection.prepareStatement(
                "DELETE FROM controlled_mobs WHERE player_uuid = ? AND mob_uuid = ?")) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, mobUUID.toString());
            pstmt.executeUpdate();

            // 从缓存中移除
            for (Map<String, Set<UUID>> playerMap : CACHE.values()) {
                for (Set<UUID> set : playerMap.values()) {
                    set.remove(mobUUID);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to delete record", e);
        }
    }

    /**
     * 获取玩家当前拥有的某种生物的数量（存活+重生中）
     */
    public static int getCurrentCount(UUID playerUUID, String mobType) {
        Map<String, Set<UUID>> playerCache = CACHE.get(playerUUID);
        if (playerCache == null) return 0;
        Set<UUID> set = playerCache.get(mobType);
        return set == null ? 0 : set.size();
    }

    /**
     * 检查是否可以再控制一只
     * @param playerUUID 玩家UUID
     * @param mobType 生物注册名
     * @param maxAllowed 最大允许数量（-1表示无限制，0表示禁止）
     * @return true 表示可以控制新的生物
     */
    public static boolean canControlMore(UUID playerUUID, String mobType, int maxAllowed) {
        if (maxAllowed == -1) return true;
        if (maxAllowed == 0) return false;
        int current = getCurrentCount(playerUUID, mobType);
        return current < maxAllowed;
    }

    public static void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOGGER.error("Failed to close database connection", e);
            }
            connection = null;
        }
        CACHE.clear();
        currentServer = null;
    }
}