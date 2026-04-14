package com.parrotservices.listeners;

import com.parrotservices.PSMagicPouch;
import com.parrotservices.data.PlayerPouchData;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Handles player join/quit (data loading/unloading) and
 * right-click interactions on upgrade kits.
 */
public class PlayerListener implements Listener {

    private final PSMagicPouch plugin;

    public PlayerListener(PSMagicPouch plugin) {
        this.plugin = plugin;
    }

    // ─── Player Join — Load Data ─────────────────────────

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        plugin.getDataManager().loadPlayer(e.getPlayer().getUniqueId());
    }

    // ─── Player Quit — Save & Unload ─────────────────────

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        java.util.UUID uuid = e.getPlayer().getUniqueId();
        PlayerPouchData data = plugin.getDataManager().getData(uuid);
        if (data != null) {
            plugin.getDataManager().saveData(uuid, data);
            plugin.getDataManager().removeFromCache(uuid);
        }
    }

    // ─── Right-Click Upgrade Kit ─────────────────────────

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        // Only handle right-clicks
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = e.getPlayer();
        ItemStack item = e.getItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();

        // ─── Check for Upgrade Kit ───
        if (meta.getPersistentDataContainer().has(PSMagicPouch.UPGRADE_KEY, PersistentDataType.INTEGER)) {
            e.setCancelled(true);

            // Global Toggle Check
            if (!plugin.getConfig().getBoolean("features.upgrades", true)) {
                return;
            }

            int targetTier = meta.getPersistentDataContainer().get(PSMagicPouch.UPGRADE_KEY, PersistentDataType.INTEGER);

            // Check if player has a pouch
            if (!plugin.getDataManager().hasData(player.getUniqueId())) {
                sendMessage(player, "no-pouch");
                playSound(player, "ENTITY_VILLAGER_NO");
                return;
            }

            PlayerPouchData data = plugin.getDataManager().getOrLoadData(player.getUniqueId());
            if (data == null) {
                sendMessage(player, "no-pouch");
                playSound(player, "ENTITY_VILLAGER_NO");
                return;
            }

            // Check if already max tier
            if (data.getTier() >= 5) {
                sendMessage(player, "max-tier");
                playSound(player, "ENTITY_VILLAGER_NO");
                return;
            }

            // Check if the kit matches the next tier
            int expectedNextTier = data.getTier() + 1;
            if (targetTier != expectedNextTier) {
                String msg = plugin.getConfigManager().getMessages().getString("messages.wrong-tier", "&cWrong tier!")
                        .replace("%current%", String.valueOf(data.getTier()))
                        .replace("%tier%", String.valueOf(targetTier));
                String prefix = getPrefix();
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + msg));
                playSound(player, "ENTITY_VILLAGER_NO");
                return;
            }

            // Apply upgrade!
            data.setTier(targetTier);
            item.setAmount(item.getAmount() - 1);
            plugin.getDataManager().saveData(player.getUniqueId(), data);

            String tierName = plugin.getConfigManager().getTiers().getString("tiers." + targetTier + ".name", "Tier " + targetTier);
            String msg = plugin.getConfigManager().getMessages().getString("messages.upgraded", "&aUpgraded!")
                    .replace("%tier%", tierName)
                    .replace("%slots%", String.valueOf(targetTier * 9));
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(getPrefix() + msg));

            String soundName = plugin.getConfig().getString("sounds.upgrade", "ENTITY_PLAYER_LEVELUP");
            playSound(player, soundName);
        }
    }

    // ─── Helpers ─────────────────────────────────────────

    private void sendMessage(Player player, String key) {
        String prefix = getPrefix();
        String msg = plugin.getConfigManager().getMessages().getString("messages." + key, "");
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + msg));
    }

    private String getPrefix() {
        return plugin.getConfigManager().getMessages().getString("prefix", "&d&l✦ &5PS-Magic Pouch &d&l✦ &r");
    }

    private void playSound(Player player, String soundName) {
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), 0.8f, 1.0f);
        } catch (IllegalArgumentException ignored) {}
    }
}
