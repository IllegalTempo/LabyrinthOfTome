package com.yourfault.perks;

import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.List;

public class ScavengerPerk extends PerkType {
    public ScavengerPerk() {
        super(
                "Scavenger",
                List.of(
                        ChatColor.GRAY + "Every little bit helps",
                        ChatColor.GRAY + "Perk Ability:",
                        ChatColor.WHITE + "Mobs you hit/kill have a 25% chance",
                        ChatColor.WHITE + "to drop extra coins (double amount)"
                ),
                PerkCategory.LEVEL,
                30,
                100,
                50
        );
    }

    @Override
    protected Material resolveIconMaterial() {
        return Material.GOLD_NUGGET;
    }
}
