package com.yourfault.Items.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class General {
    public static final ItemStack Perk_EmptySlotItem;
    static {
        ItemStack stack = new ItemStack(Material.GRAY_DYE);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_GRAY + "Empty Perk Slot");
            stack.setItemMeta(meta);
        }
        Perk_EmptySlotItem = stack;
    }
}
