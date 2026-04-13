package com.magicpouch.crafting;

import com.magicpouch.MagicPouch;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

    private final MagicPouch plugin;
    private NamespacedKey pouchRecipeKey;

    // Tier display colors (matching PouchGUI)
    private static final NamedTextColor[] TIER_COLORS = {
            NamedTextColor.WHITE, NamedTextColor.AQUA, NamedTextColor.GREEN,
            NamedTextColor.GOLD, NamedTextColor.LIGHT_PURPLE
    };
    private static final String[] TIER_NAMES = {
            "Basic", "Reinforced", "Enhanced", "Superior", "Legendary"
    };

    public PouchRecipes(MagicPouch plugin) {
        this.plugin = plugin;
    }

    // ─── Register All Recipes ────────────────────────────

    public void registerAllRecipes() {
        registerPouchRecipe();
        registerUpgradeKitRecipes();
        registerSoulLockerRecipe();
        plugin.getLogger().info("Registered 7 crafting recipes.");
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

    private ItemStack createPouchItem() {
        ItemStack item = new ItemStack(Material.LEATHER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("✦ Magic Pouch ✦").color(NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(lore("A magical pouch for portable storage!", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(lore("Tier: Basic", NamedTextColor.WHITE));
        lore.add(lore("Slots: 9", NamedTextColor.WHITE));
        lore.add(Component.empty());
        lore.add(lore("Use /pouch to open!", NamedTextColor.YELLOW));
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(MagicPouch.POUCH_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createUpgradeKitItem(int targetTier) {
        ItemStack item = new ItemStack(Material.FIREWORK_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("⬆ Upgrade Kit — " + TIER_NAMES[targetTier - 1])
                .color(TIER_COLORS[targetTier - 1])
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(lore("Upgrades your pouch to", NamedTextColor.GRAY));
        lore.add(Component.text("Tier " + targetTier + " — " + TIER_NAMES[targetTier - 1])
                .color(TIER_COLORS[targetTier - 1])
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(lore("Unlocks " + (targetTier * 9) + " storage slots", NamedTextColor.WHITE));
        lore.add(Component.empty());
        lore.add(lore("▸ Right-click to apply!", NamedTextColor.YELLOW));
        lore.add(lore("▸ Or click Upgrade in /pouch", NamedTextColor.YELLOW));
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(MagicPouch.UPGRADE_KEY, PersistentDataType.INTEGER, targetTier);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSoulLockerItem() {
        ItemStack item = new ItemStack(Material.HEART_OF_THE_SEA);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("⚡ Soul Locker ⚡").color(NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
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
        meta.getPersistentDataContainer().set(MagicPouch.LOCKER_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    // ─── Helpers ─────────────────────────────────────────

    private void sendMessage(Player player, String key) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&d&l✦ &5MagicPouch &d&l✦ &r");
        String msg = plugin.getConfig().getString("messages." + key, "");
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + msg));
    }

    private static Component lore(String text, NamedTextColor color) {
        return Component.text(text).color(color)
                .decoration(TextDecoration.ITALIC, false);
    }
}
