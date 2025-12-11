package com.yourfault.perks.perkType;

import com.yourfault.perks.PerkCategory;
import com.yourfault.perks.PerkType;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.List;

public class BrawlerPerk extends PerkType {
    public BrawlerPerk() {
        super(
                "Brawler",
                List.of(
                        ChatColor.GRAY + "You've learnt to hit where it hurts",
                        ChatColor.GRAY + "Perk Ability:",
                        ChatColor.GRAY + "Permanently gain +4 damage per level"
                ),
                PerkCategory.LEVEL,
                50,
                100,
                50,
                '\u0002'
        );
    }

    @Override
    public void applyStats(GamePlayer player, int level) {
        player.flatDamageBonus += (level * 4);
    }

    @Override
    protected Material resolveIconMaterial() {
        return Material.IRON_SWORD;
    }
}
