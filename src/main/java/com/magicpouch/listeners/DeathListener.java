package com.magicpouch.listeners;

import com.magicpouch.MagicPouch;
import com.magicpouch.data.PlayerPouchData;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles player death to determine whether pouch items are kept or dropped.
 *
 * With Soul Locker:  Items preserved, locker consumed.
 * Without Locker:    Items dropped at death location, pouch emptied.
 */
public class DeathListener implements Listener {

    private final MagicPouch plugin;

    public DeathListener(MagicPouch plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        PlayerPouchData data = plugin.getDataManager().getData(player.getUniqueId());
        if (data == null) return;

        // Check if the pouch has any items worth protecting
        if (!data.hasItems()) {
            return; // Nothing to protect or drop
        }

        if (data.hasLocker()) {
            // ── Protected: Keep items, consume locker ──
            data.setLocker(false);
            plugin.getDataManager().saveData(player.getUniqueId(), data);
            sendMessage(player, "death-protected");

            // Play dramatic protection sound with slight delay
            player.getServer().getScheduler().runTaskLater(plugin, () -> {
                String soundName = plugin.getConfig().getString("sounds.death-protected", "ITEM_TOTEM_USE");
                try {
                    player.playSound(player.getLocation(), Sound.valueOf(soundName), 1.0f, 1.0f);
                } catch (IllegalArgumentException ignored) {}
            }, 10L);

        } else {
            // ── Unprotected: Drop all pouch items at death location ──
            Location deathLoc = player.getLocation();
            int droppedCount = 0;

            for (int i = 0; i < data.getMaxSlots(); i++) {
                ItemStack item = data.getItem(i);
                if (item != null) {
                    deathLoc.getWorld().dropItemNaturally(deathLoc, item.clone());
                    data.setItem(i, null);
                    droppedCount++;
                }
            }

            plugin.getDataManager().saveData(player.getUniqueId(), data);

            if (droppedCount > 0) {
                sendMessage(player, "death-lost");
            }
        }
    }

    private void sendMessage(Player player, String key) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&d&l✦ &5MagicPouch &d&l✦ &r");
        String msg = plugin.getConfig().getString("messages." + key, "");
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + msg));
    }
}
