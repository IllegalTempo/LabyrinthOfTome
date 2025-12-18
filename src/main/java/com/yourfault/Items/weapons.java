package com.yourfault.Items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class weapons {
    public static final Map<String, ItemStack> ITEM_MAP = Map.of(
            "excalibur", EXCALIBUR(),
            "thouserhand", THOUSER()
    );
    public static ItemStack EXCALIBUR()
    {
        ItemStack result = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            // Set multi-colored display name using hex color codes (Spigot 1.16+)
            meta.setDisplayName("§x§c§8§f§1§f§eE§x§b§8§e§3§f§8x§x§a§7§d§5§f§2c§x§9§7§c§7§e§ca§x§8§6§b§9§e§7l§x§7§6§a§a§e§1i§x§6§5§9§c§d§bb§x§5§5§8§e§d§5u§x§4§4§8§0§c§fr");

            // Set lore with color gradients
            List<String> lore = Arrays.asList(
                    // First line
                    "§x§f§f§f§b§8§5P§x§f§f§f§a§8§4u§x§f§f§f§8§8§3r§x§f§f§f§7§8§1e§x§f§f§f§6§8§0 §x§f§f§f§5§7§fD§x§f§f§f§3§7§ei§x§f§f§f§2§7§cv§x§f§f§f§1§7§bi§x§f§f§f§0§7§an§x§f§f§e§e§7§9e§x§f§f§e§d§7§7,§x§f§f§e§c§7§6 §x§f§f§e§a§7§5B§x§f§f§e§9§7§4e§x§f§f§e§8§7§2y§x§f§f§e§7§7§1o§x§f§f§e§5§7§0n§x§f§f§e§4§6§fd§x§f§f§e§3§6§d §x§f§f§e§2§6§cP§x§f§f§e§0§6§bo§x§f§f§d§f§6§aw§x§f§f§d§e§6§8e§x§f§f§d§c§6§7r§x§f§f§d§b§6§6f§x§f§f§d§a§6§5u§x§f§f§d§9§6§3l§x§f§f§d§7§6§2.",
                    // Second line
                    "§x§f§f§d§5§6§0C§x§f§f§d§4§6§1a§x§f§f§d§2§5§dn§x§f§f§d§1§5§cc§x§f§f§d§0§5§br§x§f§f§c§e§5§ae§x§f§f§c§d§5§8l§x§f§f§c§c§5§7e§x§f§f§c§b§5§6a§x§f§f§c§9§5§5s§x§f§f§c§8§5§3e§x§f§f§c§7§5§2 §x§f§f§c§5§5§1s§x§f§f§c§4§5§0t§x§f§f§c§3§4§er§x§f§f§c§2§4§de§x§f§f§c§0§4§ca§x§f§f§b§f§4§bm§x§f§f§b§e§4§9s§x§f§f§b§d§4§8 §x§f§f§b§b§4§7o§x§f§f§b§a§4§6f§x§f§f§b§9§4§4 §x§f§f§b§7§4§3s§x§f§f§b§6§4§2w§x§f§f§b§5§4§1o§x§f§f§b§4§4§0r§x§f§f§b§2§3§ed§x§f§f§b§1§3§da§x§f§f§b§0§3§cu§x§f§f§a§f§3§ar§x§f§f§a§d§3§9a"
            );
            meta.setLore(lore);
            meta.setUnbreakable(true);
            CustomModelDataComponent component = meta.getCustomModelDataComponent();
            component.setStrings(List.of("excalibur","idle"));
            meta.setCustomModelDataComponent(component);

            result.setItemMeta(meta);
        }
        return result;
    }
    public static ItemStack THOUSER()
    {
        ItemStack result = new ItemStack(Material.COPPER_SWORD);
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            // Set multi-colored display name using hex color codes (Spigot 1.16+)
            meta.customName(Component.text("Thouser Hand").color(TextColor.color(255,247,166)));
            List<Component> Lore = Arrays.asList(
                    Component.text("Thousand of Hands",TextColor.color(255, 255, 255)),
                    Component.text("Attack wholeheartedly",TextColor.color(200, 200, 200))
            );
            meta.lore();
            meta.setUnbreakable(true);
            CustomModelDataComponent component = meta.getCustomModelDataComponent();
            component.setStrings(List.of("thouserhand","idle"));
            meta.setCustomModelDataComponent(component);

            result.setItemMeta(meta);
        }
        return result;
    }
}
