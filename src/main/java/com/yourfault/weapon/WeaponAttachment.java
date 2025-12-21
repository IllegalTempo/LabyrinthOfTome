package com.yourfault.weapon;

import com.yourfault.system.GeneralPlayer.GamePlayer;

public abstract class WeaponAttachment {
    protected GamePlayer player;
    protected WeaponType type;


    public WeaponAttachment(WeaponType type,GamePlayer player)
    {
        this.type = type;
        this.player = player;

    }
    public abstract void LC();
    public abstract void RC();
    public abstract void FC();
}
