package com.parrotservices.config;

import com.parrotservices.PSMagicPouch;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class ConfigManager {

    private final PSMagicPouch plugin;

    private FileConfiguration tiersConfig;
    private FileConfiguration guiConfig;
    private FileConfiguration messagesConfig;

    private File tiersFile;
    private File guiFile;
    private File messagesFile;

    public ConfigManager(PSMagicPouch plugin) {
        this.plugin = plugin;
        setup();
    }

    public void setup() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        tiersFile = new File(plugin.getDataFolder(), "tiers.yml");
        guiFile = new File(plugin.getDataFolder(), "gui.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!tiersFile.exists()) plugin.saveResource("tiers.yml", false);
        if (!guiFile.exists()) plugin.saveResource("gui.yml", false);
        if (!messagesFile.exists()) plugin.saveResource("messages.yml", false);

        reload();
    }

    public void reload() {
        tiersConfig = YamlConfiguration.loadConfiguration(tiersFile);
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        plugin.reloadConfig(); // Also reload main config
    }

    public FileConfiguration getTiers() {
        return tiersConfig;
    }

    public FileConfiguration getGUI() {
        return guiConfig;
    }

    public FileConfiguration getMessages() {
        return messagesConfig;
    }

    public void saveAll() {
        try {
            tiersConfig.save(tiersFile);
            guiConfig.save(guiFile);
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save secondary configs!", e);
        }
    }
}
