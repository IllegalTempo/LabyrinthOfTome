package com.yourfault.perks.perkType;

import com.yourfault.perks.PerkCategory;
import com.yourfault.perks.PerkType;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.List;

public class ThinkSkinPerk extends PerkType {
    public ThinkSkinPerk() {
        super(
                "Think Skin",
                List.of(
                        ChatColor.GRAY + "You're tougher than you look",
                        ChatColor.GRAY + "Perk Ability:",
                        ChatColor.WHITE + "Permanently gain +10 defense per level"
                ),
                PerkCategory.LEVEL,
                50,
                100,
                50,
                '\u0005'
        );
    }

    @Override
    public void applyStats(GamePlayer player, int level) {
        player.DEFENSE += (level * 10);
    }

    @Override
    protected Material resolveIconMaterial() {
        return Material.IRON_CHESTPLATE;
    }
}
