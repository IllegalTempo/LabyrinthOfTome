package com.yourfault.perks;

import com.yourfault.NBT_namespace;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;


public class PerkType implements Listener {

    public final String displayName;
    public final List<String> description;
    public final int menuSlot;
    public final ItemStack Icon;
    public final int cost;

    public PerkType(String displayName,
         List<String> description,
         int menuSlot,
         int cost) {
        this.displayName = displayName;
        this.description = description;
        this.menuSlot = menuSlot;
        this.Icon = GetIcon();
        this.cost = cost;
    }
    public ItemStack shop_getPerkIcon() {
        ItemStack item = Icon.clone();
        ItemMeta meta = item.getItemMeta();
        if(meta != null)
        {
            List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
            lore.add(" ");
            lore.add(ChatColor.GOLD + "Cost: " + cost + " coins");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(NBT_namespace.PERK_TYPE, PersistentDataType.STRING,displayName);
        }
        item.setItemMeta(meta);
        return item;
    }
    private ItemStack GetIcon()
    {
        ItemStack stack = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + displayName);
            meta.setLore(description);
            stack.setItemMeta(meta);
        }
        return stack;
    }
    // Registry of all perk types — list all concrete PerkType implementations here.
    // Add new perks to this array so callers can iterate through every perk.





}



