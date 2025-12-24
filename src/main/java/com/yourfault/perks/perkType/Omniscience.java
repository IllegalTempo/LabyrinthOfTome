package com.yourfault.perks.perkType;

import com.yourfault.perks.PerkCategory;
import com.yourfault.perks.PerkType;
import com.yourfault.system.GeneralPlayer.GamePlayer;

import java.util.List;

public class Omniscience extends PerkType {
    public Omniscience() {
        super("Omniscience", "Your projectile can now trace enemies", PerkCategory.LEVEL, 99, 0, 0, '\u0011');
    }

    @Override
    public void onLevelUp(GamePlayer player, int level) {
        player.homingStrength += 0.1f;
    }
}
