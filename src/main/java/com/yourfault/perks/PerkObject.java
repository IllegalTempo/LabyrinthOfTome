package com.yourfault.perks;

import com.yourfault.system.GeneralPlayer.GamePlayer;

public class PerkObject {
    public final PerkType perkType;
    private int level;
    private GamePlayer belongsTo;

    public PerkObject(PerkType perkType, GamePlayer player) {
        this.perkType = perkType;
        this.level = 1;
        this.belongsTo = player;
    }

    public int getLevel() {
        return level;
    }

    public boolean canLevelUp() {
        return level < perkType.getMaxLevel();
    }

    public boolean levelUp() {
        if (!canLevelUp()) {
            return false;
        }
        level++;

        perkType.onLevelUp(belongsTo,level);

        return true;
    }
}
