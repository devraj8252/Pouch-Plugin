package com.magicpouch.commands;

import com.magicpouch.MagicPouch;
import com.magicpouch.data.PlayerPouchData;
import com.magicpouch.gui.PouchGUI;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PouchCommand implements CommandExecutor {

    private final MagicPouch plugin;

    public PouchCommand(MagicPouch plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        // Check if the player has a pouch
        if (!plugin.getDataManager().hasData(player.getUniqueId())) {
            sendConfigMessage(player, "no-pouch");
            return true;
        }

        // Load data if not cached
        PlayerPouchData data = plugin.getDataManager().getOrLoadData(player.getUniqueId());
        if (data == null) {
            sendConfigMessage(player, "no-pouch");
            return true;
        }

        // Open the pouch GUI
        PouchGUI.openGUI(player, data, plugin);
        return true;
    }

    private void sendConfigMessage(Player player, String key) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&d&l✦ &5MagicPouch &d&l✦ &r");
        String msg = plugin.getConfig().getString("messages." + key, "&cSomething went wrong.");
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + msg));
    }
}
