// java
package com.yourfault.utils;

import org.bukkit.Color;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.ArrayList;
import java.util.List;

public class ItemUtil {
    public record ISpair(int index, String value){}

    public static ItemMeta SetCustomModelData(ItemMeta org, List<ISpair> pair) {
        if (org == null) return null;

        ItemMeta meta = org;

        CustomModelDataComponent com = meta.getCustomModelDataComponent();
        List<String> cmds = new ArrayList<String>(com.getStrings());
        for (ISpair kvp : pair)
        {
            if(kvp.index >= cmds.size())
            {
                cmds.add(kvp.index,kvp.value);

            } else {
                cmds.set(kvp.index,kvp.value);

            }
        }
        com.setStrings(cmds);
        meta.setCustomModelDataComponent(com);
        return meta;
    }
    public static ItemMeta SetItemCMDTint(ItemMeta org,long value)
    {
        ItemMeta meta = org;
        CustomModelDataComponent com = meta.getCustomModelDataComponent();
        List<Color> colorlist = new ArrayList<Color>();
        colorlist.add(Color.fromRGB((int)value));
        com.setColors(colorlist);
        meta.setCustomModelDataComponent(com);
        return meta;

    }
    public static ItemMeta SetCustomModelData(ItemMeta org, int index, String value) {
        return SetCustomModelData(org, List.of(new ISpair(index,value)));


    }
}
