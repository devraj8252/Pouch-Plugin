package com.magicpouch.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Custom InventoryHolder used to identify Pouch GUI inventories.
 * Stores the UUID of the player who owns this pouch view.
 */
public class PouchHolder implements InventoryHolder {

    private final UUID playerUUID;

    public PouchHolder(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    @Override
    public @NotNull Inventory getInventory() {
        // Not used — inventory is created separately
        return null;
    }
}
