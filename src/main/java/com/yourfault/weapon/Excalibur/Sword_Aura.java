package com.yourfault.weapon.Excalibur;

import com.yourfault.weapon.General.Projectile;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class Sword_Aura extends Projectile {
    public Sword_Aura(Location StartLocation, float damage) {
        super(StartLocation, 2, damage, 1, false, 20);
        // store the starting age set by the super constructor
        this.initialAge = this.age;

    }
    public Sword_Aura(Player p, float damage)
    {
        super(p.getEyeLocation().add(p.getLocation().getDirection()), 1, damage, 1, false, 20);
        // store the starting age set by the super constructor
        this.initialAge = this.age;
    }
    // initial lifetime preserved so we can compute elapsed ticks from inherited `age`
    private final float initialAge;

    @Override
    public void ChildUpdate()
    {
        
    }
    @Override
    public void Projectile_OnHit()
    {
        super.Projectile_OnHit();

    }
    @Override
    public void Destroy()
    {
        super.Destroy();
    }
}
