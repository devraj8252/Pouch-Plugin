package com.magicpouch.gui;

import com.magicpouch.MagicPouch;
import com.magicpouch.data.PlayerPouchData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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

    // Tier display info
    public static final String[] TIER_NAMES = {
            "Basic", "Reinforced", "Enhanced", "Superior", "Legendary"
    };
    public static final NamedTextColor[] TIER_COLORS = {
            NamedTextColor.WHITE, NamedTextColor.AQUA, NamedTextColor.GREEN,
            NamedTextColor.GOLD, NamedTextColor.LIGHT_PURPLE
    };
    private static final Material[] TIER_DYE_MATERIALS = {
            Material.WHITE_DYE, Material.LIGHT_BLUE_DYE, Material.LIME_DYE,
            Material.ORANGE_DYE, Material.MAGENTA_DYE
    };

    // ─── Open GUI ────────────────────────────────────────

    public static void openGUI(Player player, PlayerPouchData data, MagicPouch plugin) {
        PouchHolder holder = new PouchHolder(player.getUniqueId());

        // Build a rich title
        Component title = Component.text("")
                .append(Component.text("  ✦ ").color(NamedTextColor.DARK_PURPLE)
                        .decoration(TextDecoration.BOLD, true))
                .append(Component.text("Magic Pouch").color(NamedTextColor.LIGHT_PURPLE)
                        .decoration(TextDecoration.BOLD, true))
                .append(Component.text(" ✦ ").color(NamedTextColor.DARK_PURPLE)
                        .decoration(TextDecoration.BOLD, true))
                .append(Component.text(" │ ").color(NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.BOLD, false))
                .append(Component.text(TIER_NAMES[data.getTier() - 1])
                        .color(TIER_COLORS[data.getTier() - 1])
                        .decoration(TextDecoration.BOLD, true));

        Inventory inv = Bukkit.createInventory(holder, GUI_SIZE, title);

        // ─── Header Row ───
        inv.setItem(LOCKER_SLOT, createLockerDisplay(data.hasLocker()));
        inv.setItem(TIER_SLOT, createTierBadge(data.getTier()));
        for (int slot : DECO_SLOTS) {
            inv.setItem(slot, createDecoPane(data.getTier()));
        }
        inv.setItem(TITLE_SLOT, createTitleItem(data));
        inv.setItem(UPGRADE_SLOT, createUpgradeInfo(data.getTier()));
        inv.setItem(CLOSE_SLOT, createCloseButton());

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
                inv.setItem(guiSlot, createLockedSlot());
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

    public static ItemStack createLockerDisplay(boolean hasLocker) {
        if (hasLocker) {
            ItemStack item = new ItemStack(Material.HEART_OF_THE_SEA);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text("⚡ Soul Locker ⚡").color(NamedTextColor.AQUA)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(lore("✔ Active — Death Protection ON", NamedTextColor.GREEN));
            lore.add(Component.empty());
            lore.add(lore("Your pouch items will be", NamedTextColor.GRAY));
            lore.add(lore("preserved if you die.", NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(lore("⚠ Consumed on death!", NamedTextColor.YELLOW));
            lore.add(Component.empty());
            lore.add(lore("▸ Click to remove", NamedTextColor.RED));
            meta.lore(lore);
            meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
            return item;
        } else {
            ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text("🔒 No Soul Locker").color(NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(lore("✘ Unprotected", NamedTextColor.RED));
            lore.add(Component.empty());
            lore.add(lore("Your pouch items will be", NamedTextColor.GRAY));
            lore.add(lore("DROPPED if you die!", NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(lore("Craft a Soul Locker and", NamedTextColor.DARK_AQUA));
            lore.add(lore("have it in your inventory,", NamedTextColor.DARK_AQUA));
            lore.add(lore("then click here to equip!", NamedTextColor.DARK_AQUA));
            meta.lore(lore);
            item.setItemMeta(meta);
            return item;
        }
    }

    private static ItemStack createTierBadge(int tier) {
        ItemStack item = new ItemStack(TIER_DYE_MATERIALS[tier - 1]);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("★ Tier " + tier + " — " + TIER_NAMES[tier - 1] + " ★")
                .color(TIER_COLORS[tier - 1])
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(lore("Unlocked Slots: " + (tier * 9), NamedTextColor.WHITE));
        lore.add(lore("Max Slots: 45", NamedTextColor.DARK_GRAY));
        lore.add(Component.empty());

        // Progress bar
        StringBuilder bar = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            bar.append(i <= tier ? "§a■ " : "§8□ ");
        }
        lore.add(Component.text(bar.toString().trim())
                .decoration(TextDecoration.ITALIC, false));
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

    private static ItemStack createTitleItem(PlayerPouchData data) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("✦ Magic Pouch ✦").color(NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(lore("Your portable magical storage", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("  Tier: ").color(NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text(TIER_NAMES[data.getTier() - 1])
                        .color(TIER_COLORS[data.getTier() - 1])
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

    public static ItemStack createUpgradeInfo(int currentTier) {
        if (currentTier >= 5) {
            ItemStack item = new ItemStack(Material.NETHER_STAR);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text("★ MAX TIER ★").color(NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(lore("Your pouch is fully upgraded!", NamedTextColor.GREEN));
            lore.add(lore("All 45 slots are unlocked.", NamedTextColor.GREEN));
            meta.lore(lore);
            meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
            return item;
        }

        int nextTier = currentTier + 1;
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("⬆ Upgrade to " + TIER_NAMES[nextTier - 1])
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(lore("Next Tier: " + TIER_NAMES[nextTier - 1], TIER_COLORS[nextTier - 1]));
        lore.add(lore("New Slots: " + (nextTier * 9), NamedTextColor.WHITE));
        lore.add(Component.empty());
        lore.add(lore("How to Upgrade:", NamedTextColor.YELLOW));
        lore.add(lore("1. Craft the Upgrade Kit", NamedTextColor.GRAY));
        lore.add(lore("   for Tier " + nextTier, NamedTextColor.GRAY));
        lore.add(lore("2. Right-click it to apply", NamedTextColor.GRAY));
        lore.add(lore("   OR click here with it", NamedTextColor.GRAY));
        lore.add(lore("   in your inventory!", NamedTextColor.GRAY));
        lore.add(Component.empty());

        // Show required materials
        String materials = switch (nextTier) {
            case 2 -> "4× Leather + 4× Iron + Iron Block";
            case 3 -> "4× Iron + 4× Gold + Gold Block";
            case 4 -> "4× Gold + 4× Diamond + Diamond Block";
            case 5 -> "4× Diamond + 4× Netherite + Nether Star";
            default -> "";
        };
        lore.add(lore("Materials: " + materials, NamedTextColor.DARK_AQUA));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("✕ Close").color(NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(lore("Click to close the pouch", NamedTextColor.GRAY));
        lore.add(lore("Items are saved automatically!", NamedTextColor.GREEN));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ─── Locked Slot ─────────────────────────────────────

    public static ItemStack createLockedSlot() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("🔒 Locked").color(NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(lore("Upgrade your pouch to", NamedTextColor.GRAY));
        lore.add(lore("unlock this slot!", NamedTextColor.GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ─── Helper ──────────────────────────────────────────

    private static Component lore(String text, NamedTextColor color) {
        return Component.text(text).color(color)
                .decoration(TextDecoration.ITALIC, false);
    }
}
