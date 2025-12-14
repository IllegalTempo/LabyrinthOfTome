package com.yourfault.perks.perkType;

import com.yourfault.perks.PerkCategory;
import com.yourfault.perks.PerkType;
import com.yourfault.system.GeneralPlayer.GamePlayer;

import java.util.Collections;
import java.util.List;

public class GiantsBreath extends PerkType {
    public GiantsBreath() {
        super("Giant's Breath", Collections.singletonList("Giant Projectiles."), PerkCategory.LEVEL, 99, 0, 0, '\u0008');
    }

    @Override
    public void onLevelUp(GamePlayer player, int level) {
        player.projectileSizeMultiplier += 0.3f;
    }
}
