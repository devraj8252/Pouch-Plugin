package com.magicpouch.gui;

import com.magicpouch.MagicPouch;
import com.magicpouch.data.PlayerPouchData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Handles all inventory interactions within the Pouch GUI.
 * - Prevents interaction with header/decoration/locked slots
 * - Handles locker equip/unequip
 * - Handles upgrade-via-GUI
 * - Saves items on close
 * - Custom shift-click behavior
 */
public class GUIListener implements Listener {

    private final MagicPouch plugin;

    public GUIListener(MagicPouch plugin) {
        this.plugin = plugin;
    }

    // ─── Click Handler ───────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof PouchHolder holder)) return;
        if (!(e.getWhoClicked() instanceof Player player)) return;

        int rawSlot = e.getRawSlot();
        PlayerPouchData data = plugin.getDataManager().getData(holder.getPlayerUUID());
        if (data == null) return;

        // ── Click is in the player's own inventory ──
        if (rawSlot >= PouchGUI.GUI_SIZE) {
            if (e.isShiftClick() && e.getCurrentItem() != null) {
                // Custom shift-click: move to first empty unlocked storage slot
                e.setCancelled(true);
                ItemStack clickedItem = e.getCurrentItem();

                for (int i = 0; i < data.getMaxSlots(); i++) {
                    int guiSlot = PouchGUI.STORAGE_START + i;
                    ItemStack existing = e.getInventory().getItem(guiSlot);
                    if (existing == null || existing.getType() == Material.AIR) {
                        e.getInventory().setItem(guiSlot, clickedItem.clone());
                        e.setCurrentItem(null);
                        return;
                    }
                    // Try stacking with similar items
                    if (existing.isSimilar(clickedItem)) {
                        int maxStack = existing.getMaxStackSize();
                        int canAdd = maxStack - existing.getAmount();
                        if (canAdd > 0) {
                            int toMove = Math.min(canAdd, clickedItem.getAmount());
                            existing.setAmount(existing.getAmount() + toMove);
                            clickedItem.setAmount(clickedItem.getAmount() - toMove);
                            if (clickedItem.getAmount() <= 0) {
                                e.setCurrentItem(null);
                                return;
                            }
                        }
                    }
                }
                // No space — do nothing (item stays in player inventory)
            }
            return; // Allow normal interactions in player inventory
        }

        // ── Click is in the GUI ──

        // === Header Decorations — Block ===
        if (PouchGUI.DECO_SLOTS.contains(rawSlot) || rawSlot == PouchGUI.TITLE_SLOT ||
                rawSlot == PouchGUI.TIER_SLOT) {
            e.setCancelled(true);
            return;
        }

        // === Close Button ===
        if (rawSlot == PouchGUI.CLOSE_SLOT) {
            e.setCancelled(true);
            player.closeInventory();
            return;
        }

        // === Locker Slot ===
        if (rawSlot == PouchGUI.LOCKER_SLOT) {
            e.setCancelled(true);
            handleLockerClick(player, data, e.getInventory());
            return;
        }

        // === Upgrade Slot ===
        if (rawSlot == PouchGUI.UPGRADE_SLOT) {
            e.setCancelled(true);
            handleUpgradeClick(player, data);
            return;
        }

        // === Storage Slots ===
        if (rawSlot >= PouchGUI.STORAGE_START && rawSlot <= PouchGUI.STORAGE_END) {
            int storageIndex = rawSlot - PouchGUI.STORAGE_START;
            if (storageIndex >= data.getMaxSlots()) {
                // Locked slot
                e.setCancelled(true);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            }
            // Unlocked — allow normal interaction
            return;
        }

        // Catch-all: cancel any other slot
        e.setCancelled(true);
    }

    // ─── Drag Handler ────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getInventory().getHolder() instanceof PouchHolder holder)) return;

        PlayerPouchData data = plugin.getDataManager().getData(holder.getPlayerUUID());
        if (data == null) return;

        for (int slot : e.getRawSlots()) {
            // Block dragging into header
            if (slot < PouchGUI.STORAGE_START) {
                e.setCancelled(true);
                return;
            }
            // Block dragging into locked slots
            if (slot >= PouchGUI.STORAGE_START && slot <= PouchGUI.STORAGE_END) {
                int storageIndex = slot - PouchGUI.STORAGE_START;
                if (storageIndex >= data.getMaxSlots()) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    // ─── Close Handler — Save Items ──────────────────────

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof PouchHolder holder)) return;
        if (!(e.getPlayer() instanceof Player player)) return;

        PlayerPouchData data = plugin.getDataManager().getData(holder.getPlayerUUID());
        if (data == null) return;

        // Save all storage items
        Inventory inv = e.getInventory();
        for (int i = 0; i < data.getMaxSlots(); i++) {
            int guiSlot = PouchGUI.STORAGE_START + i;
            ItemStack item = inv.getItem(guiSlot);
            data.setItem(i, (item != null && item.getType() != Material.AIR) ? item.clone() : null);
        }

        plugin.getDataManager().saveData(player.getUniqueId(), data);

        // Play close sound
        String soundName = plugin.getConfig().getString("sounds.close-pouch", "BLOCK_ENDER_CHEST_CLOSE");
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), 0.6f, 1.2f);
        } catch (IllegalArgumentException ignored) {}
    }

    // ─── Locker Interaction ──────────────────────────────

    private void handleLockerClick(Player player, PlayerPouchData data, Inventory inv) {
        if (data.hasLocker()) {
            // Remove locker → give Soul Locker item back
            data.setLocker(false);
            inv.setItem(PouchGUI.LOCKER_SLOT, PouchGUI.createLockerDisplay(false));
            // Update title item
            inv.setItem(PouchGUI.TITLE_SLOT, null);

            // Give back a Soul Locker item
            ItemStack locker = createSoulLockerItem();
            player.getInventory().addItem(locker);

            sendMessage(player, "locker-removed");
            playSound(player, "sounds.remove-locker", "BLOCK_BEACON_DEACTIVATE");
        } else {
            // Try to equip — find Soul Locker in inventory
            ItemStack lockerItem = findSoulLocker(player);
            if (lockerItem == null) {
                sendMessage(player, "no-locker-in-inventory");
                playSound(player, "sounds.error", "ENTITY_VILLAGER_NO");
                return;
            }

            // Consume one locker item
            lockerItem.setAmount(lockerItem.getAmount() - 1);

            data.setLocker(true);
            inv.setItem(PouchGUI.LOCKER_SLOT, PouchGUI.createLockerDisplay(true));

            sendMessage(player, "locker-equipped");
            playSound(player, "sounds.equip-locker", "BLOCK_BEACON_ACTIVATE");
        }

        plugin.getDataManager().saveData(player.getUniqueId(), data);
    }

    // ─── Upgrade Interaction ─────────────────────────────

    private void handleUpgradeClick(Player player, PlayerPouchData data) {
        if (data.getTier() >= 5) {
            sendMessage(player, "max-tier");
            playSound(player, "sounds.error", "ENTITY_VILLAGER_NO");
            return;
        }

        int nextTier = data.getTier() + 1;

        // Find the upgrade kit for the next tier
        ItemStack upgradeKit = findUpgradeKit(player, nextTier);
        if (upgradeKit == null) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    getPrefix() + "&cYou need a &eTier " + nextTier + " Upgrade Kit &cin your inventory!"));
            playSound(player, "sounds.error", "ENTITY_VILLAGER_NO");
            return;
        }

        // Consume the upgrade kit
        upgradeKit.setAmount(upgradeKit.getAmount() - 1);

        // Save current items from GUI before upgrading
        Inventory inv = player.getOpenInventory().getTopInventory();
        for (int i = 0; i < data.getMaxSlots(); i++) {
            int guiSlot = PouchGUI.STORAGE_START + i;
            ItemStack item = inv.getItem(guiSlot);
            data.setItem(i, (item != null && item.getType() != Material.AIR) ? item.clone() : null);
        }

        // Upgrade tier
        data.setTier(nextTier);
        plugin.getDataManager().saveData(player.getUniqueId(), data);

        // Close and reopen with updated GUI
        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PouchGUI.openGUI(player, data, plugin);
            playSound(player, "sounds.upgrade", "ENTITY_PLAYER_LEVELUP");
        }, 2L);

        String msg = plugin.getConfig().getString("messages.upgraded", "&aUpgraded to %tier%!")
                .replace("%tier%", PouchGUI.TIER_NAMES[nextTier - 1])
                .replace("%slots%", String.valueOf(nextTier * 9));
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(getPrefix() + msg));
    }

    // ─── Helpers ─────────────────────────────────────────

    private ItemStack findSoulLocker(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta()) {
                if (item.getItemMeta().getPersistentDataContainer()
                        .has(MagicPouch.LOCKER_KEY, PersistentDataType.BYTE)) {
                    return item;
                }
            }
        }
        return null;
    }

    private ItemStack findUpgradeKit(Player player, int targetTier) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta()) {
                Integer tier = item.getItemMeta().getPersistentDataContainer()
                        .get(MagicPouch.UPGRADE_KEY, PersistentDataType.INTEGER);
                if (tier != null && tier == targetTier) {
                    return item;
                }
            }
        }
        return null;
    }

    private ItemStack createSoulLockerItem() {
        ItemStack item = new ItemStack(Material.HEART_OF_THE_SEA);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("⚡ Soul Locker ⚡").color(NamedTextColor.AQUA)
                .decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(MagicPouch.LOCKER_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private void sendMessage(Player player, String key) {
        String prefix = getPrefix();
        String msg = plugin.getConfig().getString("messages." + key, "");
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + msg));
    }

    private String getPrefix() {
        return plugin.getConfig().getString("messages.prefix", "&d&l✦ &5MagicPouch &d&l✦ &r");
    }

    private void playSound(Player player, String configKey, String fallback) {
        String soundName = plugin.getConfig().getString(configKey, fallback);
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), 0.6f, 1.2f);
        } catch (IllegalArgumentException ignored) {}
    }
}
