package com.parrotservices.crafting;

import com.parrotservices.PSMagicPouch;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers all crafting recipes and handles craft events.
 *
 * Recipes:
 * 1. Base Pouch:   L S L / S C S / L S L  (Leather, String, Chest)
 * 2. Upgrade Kit 2: L I L / I B I / L I L  (Leather, Iron, Iron Block)
 * 3. Upgrade Kit 3: I G I / G B G / I G I  (Iron, Gold, Gold Block)
 * 4. Upgrade Kit 4: G D G / D B D / G D G  (Gold, Diamond, Diamond Block)
 * 5. Upgrade Kit 5: D N D / N S N / D N D  (Diamond, Netherite, Nether Star)
 * 6. Soul Locker:   O E O / E S E / O E O  (Obsidian, Ender Pearl, Nether Star)
 */
public class PouchRecipes implements Listener {

    private final PSMagicPouch plugin;
    private NamespacedKey pouchRecipeKey;

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    public PouchRecipes(PSMagicPouch plugin) {
        this.plugin = plugin;
    }

    // ─── Register All Recipes ────────────────────────────

    public void registerAllRecipes() {
        if (!plugin.getConfig().getBoolean("features.crafting", true)) {
            plugin.getLogger().info("Custom crafting is globally disabled in config.");
            return;
        }

        registerPouchRecipe();

        if (plugin.getConfig().getBoolean("features.upgrades", true)) {
            registerUpgradeKitRecipes();
        }

        if (plugin.getConfig().getBoolean("features.soul-locker", true)) {
            registerSoulLockerRecipe();
        }
        
        plugin.getLogger().info("Registered crafting recipes (respecting feature toggles).");
    }

    // ─── Base Pouch Recipe ───────────────────────────────

    private void registerPouchRecipe() {
        ItemStack pouch = createPouchItem();
        pouchRecipeKey = new NamespacedKey(plugin, "base_pouch");
        ShapedRecipe recipe = new ShapedRecipe(pouchRecipeKey, pouch);
        recipe.shape("LSL", "SCS", "LSL");
        recipe.setIngredient('L', Material.LEATHER);
        recipe.setIngredient('S', Material.STRING);
        recipe.setIngredient('C', Material.CHEST);
        plugin.getServer().addRecipe(recipe);
    }

    // ─── Upgrade Kit Recipes ─────────────────────────────

    private void registerUpgradeKitRecipes() {
        // Tier 2: Reinforced
        registerUpgradeKit(2, "upgrade_kit_2",
                "LIL", "IBI", "LIL",
                new char[]{'L', 'I', 'B'},
                new Material[]{Material.LEATHER, Material.IRON_INGOT, Material.IRON_BLOCK});

        // Tier 3: Enhanced
        registerUpgradeKit(3, "upgrade_kit_3",
                "IGI", "GBG", "IGI",
                new char[]{'I', 'G', 'B'},
                new Material[]{Material.IRON_INGOT, Material.GOLD_INGOT, Material.GOLD_BLOCK});

        // Tier 4: Superior
        registerUpgradeKit(4, "upgrade_kit_4",
                "GDG", "DBD", "GDG",
                new char[]{'G', 'D', 'B'},
                new Material[]{Material.GOLD_INGOT, Material.DIAMOND, Material.DIAMOND_BLOCK});

        // Tier 5: Legendary
        registerUpgradeKit(5, "upgrade_kit_5",
                "DND", "NSN", "DND",
                new char[]{'D', 'N', 'S'},
                new Material[]{Material.DIAMOND, Material.NETHERITE_INGOT, Material.NETHER_STAR});
    }

    private void registerUpgradeKit(int tier, String keyName, String row1, String row2, String row3,
                                    char[] chars, Material[] materials) {
        ItemStack kit = createUpgradeKitItem(tier);
        NamespacedKey key = new NamespacedKey(plugin, keyName);
        ShapedRecipe recipe = new ShapedRecipe(key, kit);
        recipe.shape(row1, row2, row3);
        for (int i = 0; i < chars.length; i++) {
            recipe.setIngredient(chars[i], materials[i]);
        }
        plugin.getServer().addRecipe(recipe);
    }

    // ─── Soul Locker Recipe ──────────────────────────────

    private void registerSoulLockerRecipe() {
        ItemStack locker = createSoulLockerItem();
        NamespacedKey key = new NamespacedKey(plugin, "soul_locker");
        ShapedRecipe recipe = new ShapedRecipe(key, locker);
        recipe.shape("OEO", "ESE", "OEO");
        recipe.setIngredient('O', Material.OBSIDIAN);
        recipe.setIngredient('E', Material.ENDER_PEARL);
        recipe.setIngredient('S', Material.NETHER_STAR);
        plugin.getServer().addRecipe(recipe);
    }

    // ─── Craft Event Handlers ────────────────────────────

    /**
     * Prevent crafting a second pouch if the player already has one.
     */
    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent e) {
        if (e.getRecipe() == null) return;
        if (!(e.getRecipe() instanceof ShapedRecipe shaped)) return;

        if (shaped.getKey().equals(pouchRecipeKey)) {
            if (e.getView().getPlayer() instanceof Player player) {
                if (plugin.getDataManager().hasData(player.getUniqueId())) {
                    e.getInventory().setResult(null); // Already has a pouch
                }
            }
        }
    }

    /**
     * Register pouch data when a player crafts the base pouch.
     */
    @EventHandler
    public void onCraftItem(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getRecipe() instanceof ShapedRecipe shaped)) return;

        if (shaped.getKey().equals(pouchRecipeKey)) {
            if (plugin.getDataManager().hasData(player.getUniqueId())) {
                e.setCancelled(true);
                sendMessage(player, "already-have-pouch");
                return;
            }

            // Register the player's pouch
            plugin.getDataManager().createData(player.getUniqueId());
            sendMessage(player, "pouch-created");
        }
    }

    // ─── Item Creators ───────────────────────────────────

    public ItemStack createPouchItem() {
        String tierName = plugin.getConfigManager().getTiers().getString("tiers.1.name", "Basic");
        int slots = plugin.getConfigManager().getTiers().getInt("tiers.1.slots", 9);

        ItemStack item = new ItemStack(Material.LEATHER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize("✦ PS-Magic Pouch ✦"));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(lore("A magical pouch for portable storage!", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("&fTier: &b" + tierName).decoration(TextDecoration.ITALIC, false));
        lore.add(LEGACY.deserialize("&fSlots: &b" + slots).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(lore("Use /pouch to open!", NamedTextColor.YELLOW));
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(PSMagicPouch.POUCH_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createUpgradeKitItem(int targetTier) {
        String tierName = plugin.getConfigManager().getTiers().getString("tiers." + targetTier + ".name", "Tier " + targetTier);
        String tierColor = plugin.getConfigManager().getTiers().getString("tiers." + targetTier + ".color", "WHITE");
        int slots = plugin.getConfigManager().getTiers().getInt("tiers." + targetTier + ".slots", targetTier * 9);

        ItemStack item = new ItemStack(Material.FIREWORK_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize("&e⬆ Upgrade Kit — " + tierName));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(lore("Upgrades your pouch to", NamedTextColor.GRAY));
        lore.add(LEGACY.deserialize("&7Tier " + targetTier + " — " + tierName).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(lore("Unlocks " + slots + " storage slots", NamedTextColor.WHITE));
        lore.add(Component.empty());
        lore.add(lore("▸ Right-click to apply!", NamedTextColor.YELLOW));
        lore.add(lore("▸ Or click Upgrade in /pouch", NamedTextColor.YELLOW));
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(PSMagicPouch.UPGRADE_KEY, PersistentDataType.INTEGER, targetTier);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createSoulLockerItem() {
        ConfigurationSection section = plugin.getConfigManager().getGUI().getConfigurationSection("gui.items.locker-active");
        
        ItemStack item = new ItemStack(Material.HEART_OF_THE_SEA);
        ItemMeta meta = item.getItemMeta();
        
        if (section != null) {
            meta.displayName(LEGACY.deserialize(section.getString("name", "")));
        } else {
            meta.displayName(LEGACY.deserialize("&b&l⚡ Soul Locker ⚡"));
        }

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(lore("Protects your pouch items", NamedTextColor.GRAY));
        lore.add(lore("when you die!", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(lore("⚠ Consumed on death!", NamedTextColor.YELLOW));
        lore.add(Component.empty());
        lore.add(lore("Open /pouch and click the", NamedTextColor.DARK_AQUA));
        lore.add(lore("Locker slot to equip!", NamedTextColor.DARK_AQUA));
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(PSMagicPouch.LOCKER_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    // ─── Helpers ─────────────────────────────────────────

    private void sendMessage(Player player, String key) {
        String prefix = plugin.getConfigManager().getMessages().getString("prefix", "&d&l✦ &5PS-Magic Pouch &d&l✦ &r");
        String msg = plugin.getConfigManager().getMessages().getString("messages." + key, "");
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + msg));
    }

    private static Component lore(String text, NamedTextColor color) {
        return Component.text(text).color(color)
                .decoration(TextDecoration.ITALIC, false);
    }
}
