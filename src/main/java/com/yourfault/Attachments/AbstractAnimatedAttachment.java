package com.yourfault.Attachments;

import com.yourfault.system.LabyrinthCreature;
import com.yourfault.utils.AnimationInfo;
import com.yourfault.utils.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import static com.yourfault.Main.plugin;
import static com.yourfault.utils.ItemUtil.PlayAnimation;

public abstract class  AbstractAnimatedAttachment extends AbstractAttachment {
    public AbstractAnimatedAttachment(LabyrinthCreature parentCreature, Vector relativeOffset, ItemStack item) {
        super(parentCreature, relativeOffset, item);
    }
    public void playAnimation(AnimationInfo info)
    {
        ItemStack item = ((ArmorStand)minecraftEntity).getEquipment().getHelmet();
        if (item == null || item.getItemMeta() == null) {
            return;
        }
        ItemMeta meta = PlayAnimation(item.getItemMeta(), info.animationName(), info.durationTicks(),0);
        ((ArmorStand)minecraftEntity).getEquipment().getHelmet().setItemMeta(meta);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            item.setItemMeta(ItemUtil.SetCustomModelData(item.getItemMeta(),1,"0"));

        }, info.durationTicks());
    }
}
