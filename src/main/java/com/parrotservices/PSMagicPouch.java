package com.parrotservices;

import com.parrotservices.commands.PouchCommand;
import com.parrotservices.config.ConfigManager;
import com.parrotservices.crafting.PouchRecipes;
import com.parrotservices.data.PouchDataManager;
import com.parrotservices.gui.GUIListener;
import com.parrotservices.listeners.DeathListener;
import com.parrotservices.listeners.PlayerListener;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class PSMagicPouch extends JavaPlugin {

    private static PSMagicPouch instance;
    private PouchDataManager dataManager;
    private ConfigManager configManager;

    // PDC keys for identifying custom items
    public static NamespacedKey POUCH_KEY;
    public static NamespacedKey UPGRADE_KEY;
    public static NamespacedKey LOCKER_KEY;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        configManager = new ConfigManager(this);

        // Initialize keys
        POUCH_KEY = new NamespacedKey(this, "pouch_item");
        UPGRADE_KEY = new NamespacedKey(this, "upgrade_kit");
        LOCKER_KEY = new NamespacedKey(this, "soul_locker");

        // Initialize data manager
        dataManager = new PouchDataManager(this);

        // Register commands
        PouchCommand pouchCommand = new PouchCommand(this);
        getCommand("pouch").setExecutor(pouchCommand);
        getCommand("pouch").setTabCompleter(pouchCommand);

        // Register listeners
        PouchRecipes recipes = new PouchRecipes(this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(recipes, this);

        // Register crafting recipes
        recipes.registerAllRecipes();

        getLogger().info("✦ PS-Magic Pouch has been enabled! ✦");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAll();
        }
        getLogger().info("✦ PS-Magic Pouch has been disabled! ✦");
    }

    public static PSMagicPouch getInstance() {
        return instance;
    }

    public PouchDataManager getDataManager() {
        return dataManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
