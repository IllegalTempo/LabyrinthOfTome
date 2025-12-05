package com.yourfault.system.GeneralPlayer;

import com.yourfault.Items.gui.General;
import com.yourfault.perks.PerkObject;
import com.yourfault.perks.PerkType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class Perks {
    public final GamePlayer gamePlayer;
    public final List<PerkObject> perks = new java.util.ArrayList<>();
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
        perks.forEach(perkObject -> placeIndicator(perkObject.perkType));
    }

    private void placeIndicator(PerkType perk) {
        ItemStack indicator = buildIndicatorItem(perk);
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
    public boolean addPerk(PerkType perk)
    {
        boolean added = perks.add(new PerkObject(perk));
        if (added) {
            gamePlayer.PLAYER_TAB.updatePerkTabDisplay();
        }
        return added;
    }
    public boolean removePerk(PerkType perk) {
        PerkObject toRemove = null;
        for (PerkObject p : perks) {
            if (p.perkType.equals(perk)) {
                toRemove = p;
                break;
            }
        }
        if (toRemove == null) {
            return false;
        }
        boolean removed = perks.remove(toRemove);
        if(removed)
        {
            preparePerkSlots();
        }
        return removed;
    }
    public void clearPerks() {
        perks.clear();
    }
    public boolean hasPerk(PerkType perkClass)
    {
        for (PerkObject p : perks) {
            if (p.perkType.equals(perkClass)) return true;
        }
        return false;
    }

    public int getPerkLevel(PerkType perkType) {
        PerkObject perkObject = findPerk(perkType);
        return perkObject == null ? 0 : perkObject.getLevel();
    }

    public boolean levelUpPerk(PerkType perkType) {
        PerkObject perkObject = findPerk(perkType);
        if (perkObject == null) {
            return false;
        }
        boolean leveled = perkObject.levelUp();
        if (leveled) {
            preparePerkSlots();
        }
        return leveled;
    }

    public PerkObject findPerk(PerkType perkType) {
        for (PerkObject perkObject : perks) {
            if (perkObject.perkType.equals(perkType)) {
                return perkObject;
            }
        }
        return null;
    }

    private ItemStack buildIndicatorItem(PerkType perkType) {
        ItemStack indicator = perkType.Icon.clone();
        if (!perkType.isLevelPerk()) {
            return indicator;
        }
        PerkObject perk = findPerk(perkType);
        if (perk == null) {
            return indicator;
        }
        ItemMeta meta = indicator.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + perkType.displayName + ChatColor.GRAY + " [Lv " + perk.getLevel() + "]");
            indicator.setItemMeta(meta);
        }
        return indicator;
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
