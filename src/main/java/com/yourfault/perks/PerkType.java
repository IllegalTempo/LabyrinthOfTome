package com.yourfault.perks;

import com.yourfault.NBT_namespace;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class PerkType implements Listener {

    public final String displayName;
    public final List<String> description;
    public final ItemStack Icon;
    public final char perkimage;
    private final PerkCategory category;
    private final int maxLevel;
    private final int baseCost;
    private final int incrementalCost;

    public PerkType(String displayName,
                       List<String> description,
                       PerkCategory category,
                       char icon) {
        this(displayName, description, category,1, 0,0,icon);
    }

    public PerkType(String displayName,
                       List<String> description,
                       PerkCategory category,
                       int maxLevel,
                       int baseCost,
                       int incrementalCost,
                       char icon
    )
    {
        this.perkimage = icon;
        this.maxLevel = Math.max(1, maxLevel);
        this.displayName = displayName;
        this.description = Collections.unmodifiableList(new ArrayList<>(description));
        this.category = category;
        this.Icon = buildBaseIcon();
        this.baseCost = baseCost;
        this.incrementalCost = incrementalCost;

    }

    public PerkCategory getCategory() {
        return category;
    }

    public abstract void onLevelUp(GamePlayer player, int level);

    public boolean isLevelPerk() {
        return category == PerkCategory.LEVEL;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    //This can be overriden by child perk type
    public int costForLevel(int nextLevel) {
        if (nextLevel <= 1) {
            return 0;
        }
        int levelOffset = Math.max(0, nextLevel - 2);
        return baseCost + (levelOffset * incrementalCost);
    }

    public ItemStack buildShopIcon() {
        ItemStack item = Icon.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
            lore.add(" ");
            lore.add(ChatColor.DARK_AQUA + "Type: " + ChatColor.GOLD + category.name());
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(NBT_namespace.PERK_TYPE, PersistentDataType.STRING, displayName);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack buildLevelMenuIcon(int currentLevel, int maxLevel, int nextLevelCost) {
        ItemStack stack = Icon.clone();
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>(description);
            lore.add(" ");
            lore.add(ChatColor.DARK_GREEN + "Level: " + ChatColor.WHITE + currentLevel + "/" + maxLevel);
            if (currentLevel >= maxLevel) {
                lore.add(ChatColor.GRAY + "Max level reached");
            } else {
                lore.add(ChatColor.GOLD + "Upgrade cost: " + ChatColor.YELLOW + nextLevelCost + " coins");
            }
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(NBT_namespace.PERK_TYPE, PersistentDataType.STRING, displayName);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    protected ItemStack buildBaseIcon() {
        ItemStack stack = new ItemStack(resolveIconMaterial());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + displayName);
            meta.setLore(new ArrayList<>(description));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    protected Material resolveIconMaterial() {
        return Material.LIME_DYE;
    }
}




