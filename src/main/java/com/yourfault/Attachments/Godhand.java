package com.yourfault.Attachments;

import com.yourfault.system.LabyrinthCreature;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.util.Vector;

import java.util.List;

public class Godhand extends AbstractAnimatedAttachment{
    private static final ItemStack godhandItem = createGodhandItem();
    public Godhand(LabyrinthCreature parentCreature, Vector offset) {
        super(parentCreature, offset,godhandItem);
    }
    private static ItemStack createGodhandItem() {
        ItemStack item = new ItemStack(Material.DIAMOND_BLOCK);
        ItemMeta meta = item.getItemMeta();
        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        component.setStrings(List.of("thouserhand","animation_idle"));
        meta.setCustomModelDataComponent(component);

        return item;
    }
}
