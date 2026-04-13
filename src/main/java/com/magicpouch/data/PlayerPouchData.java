package com.magicpouch.data;

import org.bukkit.inventory.ItemStack;

/**
 * Holds all pouch-related data for a single player.
 * Tier determines how many of the 45 max storage slots are unlocked.
 */
public class PlayerPouchData {

    private int tier;
    private boolean hasLocker;
    private ItemStack[] items;

    public PlayerPouchData() {
        this.tier = 1;
        this.hasLocker = false;
        this.items = new ItemStack[45]; // 5 tiers × 9 slots each
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = Math.max(1, Math.min(5, tier));
    }

    public boolean hasLocker() {
        return hasLocker;
    }

    public void setLocker(boolean hasLocker) {
        this.hasLocker = hasLocker;
    }

    public ItemStack[] getItems() {
        return items;
    }

    public void setItems(ItemStack[] items) {
        this.items = items;
    }

    public ItemStack getItem(int slot) {
        if (slot >= 0 && slot < items.length) {
            return items[slot];
        }
        return null;
    }

    public void setItem(int slot, ItemStack item) {
        if (slot >= 0 && slot < items.length) {
            items[slot] = item;
        }
    }

    /**
     * Returns the number of unlocked storage slots based on current tier.
     */
    public int getMaxSlots() {
        return tier * 9;
    }

    /**
     * Checks if there are any items stored in the pouch.
     */
    public boolean hasItems() {
        for (ItemStack item : items) {
            if (item != null) return true;
        }
        return false;
    }

    /**
     * Clears all items from the pouch.
     */
    public void clearItems() {
        items = new ItemStack[45];
    }
}
