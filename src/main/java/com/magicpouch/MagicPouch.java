package com.magicpouch;

import com.magicpouch.commands.PouchCommand;
import com.magicpouch.crafting.PouchRecipes;
import com.magicpouch.data.PouchDataManager;
import com.magicpouch.gui.GUIListener;
import com.magicpouch.listeners.DeathListener;
import com.magicpouch.listeners.PlayerListener;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class MagicPouch extends JavaPlugin {

    private static MagicPouch instance;
    private PouchDataManager dataManager;

    // PDC keys for identifying custom items
    public static NamespacedKey POUCH_KEY;
    public static NamespacedKey UPGRADE_KEY;
    public static NamespacedKey LOCKER_KEY;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Initialize keys
        POUCH_KEY = new NamespacedKey(this, "pouch_item");
        UPGRADE_KEY = new NamespacedKey(this, "upgrade_kit");
        LOCKER_KEY = new NamespacedKey(this, "soul_locker");

        // Initialize data manager
        dataManager = new PouchDataManager(this);

        // Register commands
        getCommand("pouch").setExecutor(new PouchCommand(this));

        // Register listeners
        PouchRecipes recipes = new PouchRecipes(this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(recipes, this);

        // Register crafting recipes
        recipes.registerAllRecipes();

        getLogger().info("✦ MagicPouch has been enabled! ✦");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAll();
        }
        getLogger().info("✦ MagicPouch has been disabled! ✦");
    }

    public static MagicPouch getInstance() {
        return instance;
    }

    public PouchDataManager getDataManager() {
        return dataManager;
    }
}
