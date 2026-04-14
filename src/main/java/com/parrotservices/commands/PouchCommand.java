package com.parrotservices.commands;

import com.parrotservices.PSMagicPouch;
import com.parrotservices.crafting.PouchRecipes;
import com.parrotservices.data.PlayerPouchData;
import com.parrotservices.gui.PouchGUI;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PouchCommand implements CommandExecutor, TabCompleter {

    private final PSMagicPouch plugin;

    public PouchCommand(PSMagicPouch plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        // ─── Admin Commands ───
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("magicpouch.admin")) {
                    sendMessage(sender, "no-permission");
                    return true;
                }
                plugin.getConfigManager().reload();
                sendMessage(sender, "reloaded");
                return true;
            }

            if (args[0].equalsIgnoreCase("admin") || args[0].equalsIgnoreCase("give")) {
                if (!sender.hasPermission("magicpouch.admin")) {
                    sendMessage(sender, "no-permission");
                    return true;
                }
                return handleAdminGive(sender, args);
            }
        }

        // ─── Default Command: Open Pouch ───
        if (!(sender instanceof Player player)) {
            sendMessage(sender, "player-only");
            return true;
        }

        if (!plugin.getDataManager().hasData(player.getUniqueId())) {
            sendMessage(player, "no-pouch");
            return true;
        }

        PlayerPouchData data = plugin.getDataManager().getOrLoadData(player.getUniqueId());
        if (data == null) {
            sendMessage(player, "no-pouch");
            return true;
        }

        PouchGUI.openGUI(player, data, plugin);
        return true;
    }

    private boolean handleAdminGive(CommandSender sender, String[] args) {
        // args: [admin, give, target, item, tier] OR [give, target, item, tier]
        int offset = args[0].equalsIgnoreCase("admin") ? 1 : 0;
        
        if (args.length < offset + 3) {
            sendMessage(sender, "invalid-args");
            return true;
        }

        Player target = Bukkit.getPlayer(args[offset + 1]);
        if (target == null) {
            sendMessage(sender, "player-not-found");
            return true;
        }

        String itemType = args[offset + 2].toLowerCase();
        ItemStack itemStack = null;
        PouchRecipes recipes = new PouchRecipes(plugin);

        switch (itemType) {
            case "pouch" -> itemStack = recipes.createPouchItem();
            case "locker" -> itemStack = recipes.createSoulLockerItem();
            case "upgrade" -> {
                int tier = 2;
                if (args.length > offset + 3) {
                    try {
                        tier = Integer.parseInt(args[offset + 3]);
                    } catch (NumberFormatException ignored) {}
                }
                if (tier < 2 || tier > 5) {
                    sender.sendMessage("§cTier must be between 2 and 5!");
                    return true;
                }
                itemStack = recipes.createUpgradeKitItem(tier);
            }
            default -> {
                sendMessage(sender, "invalid-args");
                return true;
            }
        }

        if (itemStack != null) {
            target.getInventory().addItem(itemStack);
            String itemMsg = itemType.substring(0,1).toUpperCase() + itemType.substring(1);
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    getPrefix() + "§aYou gave §e" + target.getName() + " §aa §e" + itemMsg + "§a!"));
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("magicpouch.admin")) return completions;

        if (args.length == 1) {
            completions.add("reload");
            completions.add("give");
            completions.add("admin");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("admin"))) {
             if (args[0].equalsIgnoreCase("admin")) {
                 completions.add("give");
             } else {
                 return null; // Return player names
             }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            return null; // Return player names
        } else if ((args.length == 3 && args[0].equalsIgnoreCase("give")) || (args.length == 4 && args[0].equalsIgnoreCase("admin"))) {
            completions.add("pouch");
            completions.add("locker");
            completions.add("upgrade");
        } else if ((args.length == 4 && args[0].equalsIgnoreCase("give") && args[2].equalsIgnoreCase("upgrade")) ||
                   (args.length == 5 && args[0].equalsIgnoreCase("admin") && args[3].equalsIgnoreCase("upgrade"))) {
            completions.add("2");
            completions.add("3");
            completions.add("4");
            completions.add("5");
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream().filter(s -> s.startsWith(lastArg)).collect(Collectors.toList());
    }

    private void sendMessage(CommandSender sender, String key) {
        String msg = plugin.getConfigManager().getMessages().getString("messages." + key, "");
        if (msg.isEmpty()) return;
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(getPrefix() + msg));
    }

    private String getPrefix() {
        return plugin.getConfigManager().getMessages().getString("prefix", "&d&l✦ &5PS-Magic Pouch &d&l✦ &r");
    }
}
