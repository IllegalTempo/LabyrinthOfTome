package com.yourfault.perk.sharpshooter;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class SharpshooterPerk {
    public static final int MENU_SLOT = 15;
    public static final String DISPLAY_NAME = "Sharpshooter";
    public static final String DESCRIPTION = "A steady hand and a keen eye";

    private static final List<String> ABILITY_LORE = List.of(
            ChatColor.GRAY + "Perk Ability:",
            ChatColor.WHITE + "Arrows travel 25% faster",
            ChatColor.WHITE + "Deal +3 bow damage"
    );

    private static final ItemStack MENU_TEMPLATE;
    private static final ItemStack INDICATOR_TEMPLATE;

    static {
        MENU_TEMPLATE = buildBaseIcon();
        INDICATOR_TEMPLATE = buildBaseIcon();
    }

    private SharpshooterPerk() {}

    public static ItemStack buildMenuIcon(NamespacedKey key, String perkId) {
        ItemStack stack = MENU_TEMPLATE.clone();
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, perkId);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static ItemStack buildIndicatorIcon() {
        return INDICATOR_TEMPLATE.clone();
    }

    private static ItemStack buildBaseIcon() {
        ItemStack stack = new ItemStack(Material.LAPIS_LAZULI);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + DISPLAY_NAME);
            meta.setLore(buildLore());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static List<String> buildLore() {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + DESCRIPTION);
        lore.add(" ");
        lore.addAll(ABILITY_LORE);
        return lore;
    }
}

