package com.yourfault.handler;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.yourfault.perk.PerkType;
import com.yourfault.system.Player;

public class PerkSelectionHandler {
    private static final int[] PERK_SLOT_INDEXES = {9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
    private static final int SELECTOR_SLOT = 8;

    private final HashMap<UUID, Player> playerList;
    private final ItemStack emptySlotItem;

    public PerkSelectionHandler(HashMap<UUID, Player> playerList) {
        this.playerList = playerList;
        this.emptySlotItem = createEmptySlotItem();
    }

    public void preparePerkSlots(org.bukkit.entity.Player bukkitPlayer) {
        Player player = resolveOrCreatePlayer(bukkitPlayer);
        resetIndicators(bukkitPlayer);
        reapplyIndicators(bukkitPlayer, player);
    }

    public boolean applyPerkSelection(org.bukkit.entity.Player bukkitPlayer, PerkType perkType) {
        Player player = resolveOrCreatePlayer(bukkitPlayer);
        boolean added = player.addPerk(perkType);
        if (added) {
            placeIndicator(bukkitPlayer, perkType);
        }
        return added;
    }

    public boolean hasPerk(UUID uuid, PerkType perkType) {
        Player player = playerList.get(uuid);
        return player != null && player.hasPerk(perkType);
    }

    private Player resolveOrCreatePlayer(org.bukkit.entity.Player bukkitPlayer) {
        UUID uuid = bukkitPlayer.getUniqueId();
        Player existing = playerList.get(uuid);
        if (existing == null) {
            existing = new Player(bukkitPlayer, 100f, 100f, 100f, null);
            playerList.put(uuid, existing);
        } else {
            existing.setMinecraftPlayer(bukkitPlayer);
        }
        return existing;
    }

    private void resetIndicators(org.bukkit.entity.Player bukkitPlayer) {
        for (int slot : PERK_SLOT_INDEXES) {
            bukkitPlayer.getInventory().setItem(slot, emptySlotItem.clone());
        }
    }

    public void clearPerkIndicators(org.bukkit.entity.Player bukkitPlayer) {
        for (int slot : PERK_SLOT_INDEXES) {
            bukkitPlayer.getInventory().setItem(slot, null);
        }
        bukkitPlayer.getInventory().setItem(SELECTOR_SLOT, null);
    }

    public void removePerks(org.bukkit.entity.Player bukkitPlayer) {
        Player player = playerList.get(bukkitPlayer.getUniqueId());
        if (player != null) {
            player.clearPerks();
        }
        clearPerkIndicators(bukkitPlayer);
    }

    private void reapplyIndicators(org.bukkit.entity.Player bukkitPlayer, Player player) {
        player.getPerks().forEach(perk -> placeIndicator(bukkitPlayer, perk));
    }

    private void placeIndicator(org.bukkit.entity.Player bukkitPlayer, PerkType perkType) {
        ItemStack indicator = perkType.buildIndicatorIcon();
        for (int slot : PERK_SLOT_INDEXES) {
            ItemStack current = bukkitPlayer.getInventory().getItem(slot);
            if (current == null || current.isSimilar(emptySlotItem)) {
                bukkitPlayer.getInventory().setItem(slot, indicator.clone());
                return;
            }
        }
        bukkitPlayer.getInventory().addItem(indicator.clone());
    }

    private ItemStack createEmptySlotItem() {
        ItemStack stack = new ItemStack(Material.GRAY_DYE);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_GRAY + "Empty Perk Slot");
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
