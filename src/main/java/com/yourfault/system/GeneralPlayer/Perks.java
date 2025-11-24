package com.yourfault.system.GeneralPlayer;

import com.yourfault.Items.gui.General;
import com.yourfault.perks.PerkType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class Perks {
    public final GamePlayer gamePlayer;
    private final EnumSet<PerkType> perks = EnumSet.noneOf(PerkType.class);
    private static final int[] PERK_SLOT_INDEXES = {9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
    private static final int SELECTOR_SLOT = 8;


    public Perks(GamePlayer gamePlayer) {
        this.gamePlayer = gamePlayer;
    }

    public void preparePerkSlots() {
        resetIndicators();
        reapplyIndicators();
    }
    public boolean applyPerkSelection(PerkType perkType) {
        boolean added = addPerk(perkType);
        if (added) {
            placeIndicator(perkType);
        }
        return added;
    }
    private void resetIndicators() {
        for (int slot : PERK_SLOT_INDEXES) {
            gamePlayer.getMinecraftPlayer().getInventory().setItem(slot, General.Perk_EmptySlotItem.clone());
        }
    }
    public void clearPerkIndicators() {
        Player bukkitPlayer = gamePlayer.getMinecraftPlayer();
        for (int slot : PERK_SLOT_INDEXES) {
            bukkitPlayer.getInventory().setItem(slot, null);
        }
        bukkitPlayer.getInventory().setItem(SELECTOR_SLOT, null);
    }
    public void removePerks() {
        clearPerks();
        clearPerkIndicators();
    }

    private void reapplyIndicators() {
        getPerks().forEach(this::placeIndicator);
    }

    private void placeIndicator(PerkType perkType) {
        ItemStack indicator = perkType.buildIndicatorIcon();
        Player bukkitPlayer = gamePlayer.getMinecraftPlayer();
        for (int slot : PERK_SLOT_INDEXES) {
            ItemStack current = bukkitPlayer.getInventory().getItem(slot);
            if (current == null || current.isSimilar( General.Perk_EmptySlotItem)) {
                bukkitPlayer.getInventory().setItem(slot, indicator.clone());
                return;
            }
        }
        bukkitPlayer.getInventory().addItem(indicator.clone());
    }
    public boolean addPerk(PerkType perk) {
        return perks.add(perk);
    }
    public boolean removePerk(PerkType perk) {
        boolean removed = perks.remove(perk);
        if (removed) {
            preparePerkSlots();
        }
        return removed;
    }
    public boolean hasPerk(PerkType perk) {
        return perks.contains(perk);
    }
    public Set<PerkType> getPerks() {
        return Collections.unmodifiableSet(perks);
    }
    public void clearPerks() {
        perks.clear();
    }
    public boolean HasPerk(PerkType perk)
    {
        return perks.contains(perk);
    }

    public static boolean isPerkSlot(int slot) {
        for (int index : PERK_SLOT_INDEXES) {
            if (index == slot) {
                return true;
            }
        }
        return false;
    }
}
