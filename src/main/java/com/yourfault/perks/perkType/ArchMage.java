package com.yourfault.perks.perkType;

import com.yourfault.perks.PerkCategory;
import com.yourfault.perks.PerkType;
import com.yourfault.system.GeneralPlayer.GamePlayer;

import java.util.List;

public class ArchMage extends PerkType {


    public ArchMage() {
        super("Arch Mage", "Increase Max Mana and Mana Regen Rate", PerkCategory.LEVEL, 99, 0, 0, '\u0010');
    }

    @Override
    public void onLevelUp(GamePlayer player, int level) {
        player.MAX_MANA += 20;
        player.manaRegenRate += 0.1f;
    }
}
