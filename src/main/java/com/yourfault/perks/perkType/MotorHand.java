package com.yourfault.perks.perkType;

import com.yourfault.perks.PerkCategory;
import com.yourfault.perks.PerkType;
import com.yourfault.system.GeneralPlayer.GamePlayer;

import java.util.Collections;
import java.util.List;

public class MotorHand extends PerkType {
    public MotorHand() {
        super("Motor Hand", Collections.singletonList("Increase your attack speed"), PerkCategory.LEVEL, 99, 0, 0, '\u0009');
    }

    @Override
    public void onLevelUp(GamePlayer player, int level) {
        player.attackSpeed += 2; //2tick
    }
}
