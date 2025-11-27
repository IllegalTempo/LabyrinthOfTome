package com.yourfault.perks;

public class PerkObject {
    public PerkType perkType;
    public int level;
    public PerkObject(PerkType perkType) {
        this.perkType = perkType;
        this.level = 1;
    }
}
