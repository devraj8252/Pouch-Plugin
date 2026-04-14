package com.parrotservices.gui;

import com.parrotservices.PSMagicPouch;
import com.parrotservices.data.PlayerPouchData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Creates and manages the unique Pouch GUI layout.
 *
 * Layout (54 slots, 6 rows):
 * ┌─────────────────────────────────────────────────────┐
 * │ [Locker] [Tier] [◆] [◆] [✦Title✦] [◆] [◆] [⬆] [✕] │  Row 0: Header
 * │ [S] [S] [S] [S] [S] [S] [S] [S] [S]               │  Row 1: Storage (Tier 1+)
 * │ [S] [S] [S] [S] [S] [S] [S] [S] [S]               │  Row 2: Storage (Tier 2+)
 * │ [S] [S] [S] [S] [S] [S] [S] [S] [S]               │  Row 3: Storage (Tier 3+)
 * │ [S] [S] [S] [S] [S] [S] [S] [S] [S]               │  Row 4: Storage (Tier 4+)
 * │ [S] [S] [S] [S] [S] [S] [S] [S] [S]               │  Row 5: Storage (Tier 5)
 * └─────────────────────────────────────────────────────┘
 * [S] = Storage slot, unlocked rows based on tier
 * Locked rows show gray glass panes with "🔒 Locked" label
 */
public class PouchGUI {

    public static final int GUI_SIZE = 54;

    // Header slot positions
    public static final int LOCKER_SLOT = 0;
    public static final int TIER_SLOT = 1;
    public static final int TITLE_SLOT = 4;
    public static final int UPGRADE_SLOT = 7;
    public static final int CLOSE_SLOT = 8;
    public static final Set<Integer> DECO_SLOTS = Set.of(2, 3, 5, 6);
    public static final Set<Integer> HEADER_SLOTS = Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8);

    // Storage area
    public static final int STORAGE_START = 9;
    public static final int STORAGE_END = 53;

    // Helper for legacy colors
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    // ─── Open GUI ────────────────────────────────────────

    public static void openGUI(Player player, PlayerPouchData data, PSMagicPouch plugin) {
        PouchHolder holder = new PouchHolder(player.getUniqueId());
        FileConfiguration tiersConfig = plugin.getConfigManager().getTiers();
        FileConfiguration guiConfig = plugin.getConfigManager().getGUI();

        // Build a rich title from config
        String tierName = tiersConfig.getString("tiers." + data.getTier() + ".name", "Unknown");
        String titleStr = guiConfig.getString("gui.title", "Magic Pouch │ %tier_name%")
                .replace("%tier_name%", tierName);
        Component title = LEGACY.deserialize(titleStr);

        Inventory inv = Bukkit.createInventory(holder, GUI_SIZE, title);

        // ─── Header Row ───
        if (plugin.getConfig().getBoolean("features.soul-locker", true)) {
            inv.setItem(LOCKER_SLOT, createLockerDisplay(data.hasLocker(), plugin));
        } else {
            inv.setItem(LOCKER_SLOT, createDecoPane(data.getTier()));
        }

        inv.setItem(TIER_SLOT, createTierBadge(data.getTier(), plugin));
        for (int slot : DECO_SLOTS) {
            inv.setItem(slot, createDecoPane(data.getTier()));
        }
        inv.setItem(TITLE_SLOT, createTitleItem(data, plugin));

        if (plugin.getConfig().getBoolean("features.upgrades", true)) {
            inv.setItem(UPGRADE_SLOT, createUpgradeInfo(data.getTier(), plugin));
        } else {
            inv.setItem(UPGRADE_SLOT, createDecoPane(data.getTier()));
        }
        
        inv.setItem(CLOSE_SLOT, createCloseButton(plugin));

        // ─── Storage Rows ───
        int maxSlots = data.getMaxSlots();
        for (int i = 0; i < 45; i++) {
            int guiSlot = STORAGE_START + i;
            if (i < maxSlots) {
                // Unlocked — place stored item (or leave null for empty)
                ItemStack stored = data.getItem(i);
                if (stored != null) {
                    inv.setItem(guiSlot, stored.clone());
                }
            } else {
                // Locked
                inv.setItem(guiSlot, createLockedSlot(plugin));
            }
        }

        player.openInventory(inv);

        // Play open sound
        String soundName = plugin.getConfig().getString("sounds.open-pouch", "BLOCK_ENDER_CHEST_OPEN");
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), 0.6f, 1.2f);
        } catch (IllegalArgumentException ignored) {}
    }

    // ─── Header Item Builders ────────────────────────────

    public static ItemStack createLockerDisplay(boolean hasLocker, PSMagicPouch plugin) {
        String path = hasLocker ? "gui.items.locker-active" : "gui.items.locker-inactive";
        ConfigurationSection section = plugin.getConfigManager().getGUI().getConfigurationSection(path);
        
        if (section == null) {
            // Fallback
            return new ItemStack(hasLocker ? Material.HEART_OF_THE_SEA : Material.RED_STAINED_GLASS_PANE);
        }

        ItemStack item = new ItemStack(Material.matchMaterial(section.getString("material", "BARRIER")));
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(LEGACY.deserialize(section.getString("name", "")));
        List<Component> lore = new ArrayList<>();
        for (String line : section.getStringList("lore")) {
            lore.add(LEGACY.deserialize(line).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        if (hasLocker) meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createTierBadge(int tier, PSMagicPouch plugin) {
        FileConfiguration tiersConfig = plugin.getConfigManager().getTiers();
        String tierName = tiersConfig.getString("tiers." + tier + ".name", "Tier " + tier);
        String tierColorStr = tiersConfig.getString("tiers." + tier + ".color", "WHITE");
        Material material = Material.matchMaterial(tiersConfig.getString("tiers." + tier + ".material", "WHITE_DYE"));

        ItemStack item = new ItemStack(material != null ? material : Material.WHITE_DYE);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(LEGACY.deserialize("&e★ " + tierName + " ★"));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(lore("Unlocked Slots: " + (tiersConfig.getInt("tiers." + tier + ".slots", tier * 9)), NamedTextColor.WHITE));
        lore.add(lore("Max Slots: 45", NamedTextColor.DARK_GRAY));
        lore.add(Component.empty());

        // Progress bar
        StringBuilder bar = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            bar.append(i <= tier ? "§a■ " : "§8□ ");
        }
        lore.add(Component.text(bar.toString().trim()).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        if (tier >= 5) {
            meta.setEnchantmentGlintOverride(true);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createDecoPane(int tier) {
        Material paneMaterial;
        switch (tier) {
            case 1 -> paneMaterial = Material.WHITE_STAINED_GLASS_PANE;
            case 2 -> paneMaterial = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case 3 -> paneMaterial = Material.LIME_STAINED_GLASS_PANE;
            case 4 -> paneMaterial = Material.ORANGE_STAINED_GLASS_PANE;
            default -> paneMaterial = Material.MAGENTA_STAINED_GLASS_PANE;
        }
        ItemStack item = new ItemStack(paneMaterial);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of());
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createTitleItem(PlayerPouchData data, PSMagicPouch plugin) {
        FileConfiguration tiersConfig = plugin.getConfigManager().getTiers();
        String tierName = tiersConfig.getString("tiers." + data.getTier() + ".name", "Unknown");
        String tierColorStr = tiersConfig.getString("tiers." + data.getTier() + ".color", "WHITE");
        NamedTextColor tierColor = NamedTextColor.NAMES.value(tierColorStr.toLowerCase());
        if (tierColor == null) tierColor = NamedTextColor.WHITE;

        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize("✦ PS-Magic Pouch ✦"));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(lore("Your portable magical storage", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("  Tier: ").color(NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text(tierName)
                        .color(tierColor)
                        .decoration(TextDecoration.BOLD, true)));
        lore.add(Component.text("  Slots: ").color(NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text(data.getMaxSlots() + "/45")
                        .color(NamedTextColor.WHITE)));
        lore.add(Component.text("  Locker: ").color(NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false)
                .append(data.hasLocker()
                        ? Component.text("✔ Protected").color(NamedTextColor.GREEN)
                        : Component.text("✘ None").color(NamedTextColor.RED)));
        lore.add(Component.empty());
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createUpgradeInfo(int currentTier, PSMagicPouch plugin) {
        FileConfiguration tiersConfig = plugin.getConfigManager().getTiers();
        FileConfiguration guiConfig = plugin.getConfigManager().getGUI();
        if (currentTier >= 5) {
            String path = "gui.items.max-tier";
            ConfigurationSection section = guiConfig.getConfigurationSection(path);
            if (section == null) return new ItemStack(Material.NETHER_STAR);

            ItemStack item = new ItemStack(Material.matchMaterial(section.getString("material", "NETHER_STAR")));
            ItemMeta meta = item.getItemMeta();
            meta.displayName(LEGACY.deserialize(section.getString("name", "")));
            List<Component> lore = new ArrayList<>();
            for (String line : section.getStringList("lore")) {
                lore.add(LEGACY.deserialize(line).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
            return item;
        }

        int nextTier = currentTier + 1;
        String path = "gui.items.upgrade-info";
        ConfigurationSection section = guiConfig.getConfigurationSection(path);
        if (section == null) return new ItemStack(Material.ANVIL);

        String nextTierName = tiersConfig.getString("tiers." + nextTier + ".name", "Tier " + nextTier);
        String nextTierColor = tiersConfig.getString("tiers." + nextTier + ".color", "WHITE");
        int nextTierSlots = tiersConfig.getInt("tiers." + nextTier + ".slots", nextTier * 9);
        String materials = tiersConfig.getString("tiers." + nextTier + ".upgrade-requirement", "Unknown");

        ItemStack item = new ItemStack(Material.matchMaterial(section.getString("material", "ANVIL")));
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(LEGACY.deserialize(section.getString("name", "")
                .replace("%next_tier_name%", nextTierName)));

        List<Component> lore = new ArrayList<>();
        for (String line : section.getStringList("lore")) {
            String processed = line
                    .replace("%next_tier_name%", nextTierName)
                    .replace("%next_tier_color%", "&" + getLegacyChar(nextTierColor))
                    .replace("%next_tier_slots%", String.valueOf(nextTierSlots))
                    .replace("%next_tier_num%", String.valueOf(nextTier))
                    .replace("%materials%", materials);
            lore.add(LEGACY.deserialize(processed).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createCloseButton(PSMagicPouch plugin) {
        ConfigurationSection section = plugin.getConfigManager().getGUI().getConfigurationSection("gui.items.close-button");
        if (section == null) return new ItemStack(Material.BARRIER);

        ItemStack item = new ItemStack(Material.matchMaterial(section.getString("material", "BARRIER")));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize(section.getString("name", "")));
        List<Component> lore = new ArrayList<>();
        for (String line : section.getStringList("lore")) {
            lore.add(LEGACY.deserialize(line).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ─── Locked Slot ─────────────────────────────────────

    public static ItemStack createLockedSlot(PSMagicPouch plugin) {
        ConfigurationSection section = plugin.getConfigManager().getGUI().getConfigurationSection("gui.items.locked-slot");
        if (section == null) return new ItemStack(Material.GRAY_STAINED_GLASS_PANE);

        ItemStack item = new ItemStack(Material.matchMaterial(section.getString("material", "GRAY_STAINED_GLASS_PANE")));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize(section.getString("name", "")));
        List<Component> lore = new ArrayList<>();
        for (String line : section.getStringList("lore")) {
            lore.add(LEGACY.deserialize(line).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ─── Helpers ──────────────────────────────────────────

    private static String getLegacyChar(String colorName) {
        return switch (colorName.toUpperCase()) {
            case "AQUA" -> "b";
            case "BLACK" -> "0";
            case "BLUE" -> "9";
            case "DARK_AQUA" -> "3";
            case "DARK_BLUE" -> "1";
            case "DARK_GRAY" -> "8";
            case "DARK_GREEN" -> "2";
            case "DARK_PURPLE" -> "5";
            case "DARK_RED" -> "4";
            case "GOLD" -> "6";
            case "GRAY" -> "7";
            case "GREEN" -> "a";
            case "LIGHT_PURPLE" -> "d";
            case "RED" -> "c";
            case "YELLOW" -> "e";
            default -> "f";
        };
    }

    private static Component lore(String text, NamedTextColor color) {
        return Component.text(text).color(color)
                .decoration(TextDecoration.ITALIC, false);
    }
}
