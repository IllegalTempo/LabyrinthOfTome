package com.yourfault.perks.perkType;

import com.yourfault.perks.PerkCategory;
import com.yourfault.perks.PerkType;
import com.yourfault.system.GeneralPlayer.GamePlayer;

import java.util.Collections;
import java.util.List;

public class ArrowRain extends PerkType {
    public ArrowRain() {
        super("Arrow Rain", Collections.singletonList("Increase Projectile Multiplier"), PerkCategory.LEVEL, 99, 0, 0, '\u0007');
    }
    @Override
    public void onLevelUp(GamePlayer player, int level) {
        player.projectileMultiplier += 0.5f;
    }
}
