package com.parrotservices.data;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages loading, saving, and caching player pouch data.
 * Each player's data is stored in a separate YAML file: data/<uuid>.yml
 */
public class PouchDataManager {

    private final JavaPlugin plugin;
    private final File dataFolder;
    private final Map<UUID, PlayerPouchData> cache;

    public PouchDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        this.cache = new ConcurrentHashMap<>();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    /**
     * Gets cached data for a player, or null if not loaded/exists.
     */
    public PlayerPouchData getData(UUID uuid) {
        return cache.get(uuid);
    }

    /**
     * Gets data from cache, loading from disk if needed.
     */
    public PlayerPouchData getOrLoadData(UUID uuid) {
        PlayerPouchData data = cache.get(uuid);
        if (data == null) {
            data = loadFromDisk(uuid);
            if (data != null) {
                cache.put(uuid, data);
            }
        }
        return data;
    }

    /**
     * Creates new pouch data for a player (Tier 1, no locker, empty items).
     */
    public void createData(UUID uuid) {
        PlayerPouchData data = new PlayerPouchData();
        cache.put(uuid, data);
        saveToDisk(uuid, data);
    }

    /**
     * Checks if a player has pouch data (in cache or on disk).
     */
    public boolean hasData(UUID uuid) {
        if (cache.containsKey(uuid)) return true;
        File file = new File(dataFolder, uuid + ".yml");
        return file.exists();
    }

    /**
     * Loads player data from disk into cache.
     */
    public void loadPlayer(UUID uuid) {
        PlayerPouchData data = loadFromDisk(uuid);
        if (data != null) {
            cache.put(uuid, data);
        }
    }

    /**
     * Saves a specific player's data to disk.
     */
    public void saveData(UUID uuid, PlayerPouchData data) {
        saveToDisk(uuid, data);
    }

    /**
     * Saves all cached data to disk (called on plugin disable).
     */
    public void saveAll() {
        for (Map.Entry<UUID, PlayerPouchData> entry : cache.entrySet()) {
            saveToDisk(entry.getKey(), entry.getValue());
        }
        plugin.getLogger().info("Saved " + cache.size() + " player pouch data files.");
    }

    /**
     * Removes a player from the cache (call on quit to prevent memory leaks).
     */
    public void removeFromCache(UUID uuid) {
        cache.remove(uuid);
    }

    // ─── Internal disk I/O ───────────────────────────────────

    private PlayerPouchData loadFromDisk(UUID uuid) {
        File file = new File(dataFolder, uuid + ".yml");
        if (!file.exists()) return null;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        PlayerPouchData data = new PlayerPouchData();
        data.setTier(config.getInt("tier", 1));
        data.setLocker(config.getBoolean("has-locker", false));

        if (config.isConfigurationSection("items")) {
            for (String key : config.getConfigurationSection("items").getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ItemStack item = config.getItemStack("items." + key);
                    if (item != null) {
                        data.setItem(slot, item);
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid slot key in " + uuid + ".yml: " + key);
                }
            }
        }

        return data;
    }

    private void saveToDisk(UUID uuid, PlayerPouchData data) {
        File file = new File(dataFolder, uuid + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("tier", data.getTier());
        config.set("has-locker", data.hasLocker());

        // Clear items section first, then write non-null items
        config.set("items", null);
        ItemStack[] items = data.getItems();
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                config.set("items." + i, items[i]);
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save pouch data for " + uuid, e);
        }
    }
}
