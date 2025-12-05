package com.yourfault.perks;

public class PerkObject {
    public final PerkType perkType;
    private int level;

    public PerkObject(PerkType perkType) {
        this.perkType = perkType;
        this.level = 1;
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
        return true;
    }
}
