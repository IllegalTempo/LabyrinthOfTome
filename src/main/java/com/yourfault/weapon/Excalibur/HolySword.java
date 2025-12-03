package com.yourfault.weapon.Excalibur;

import com.yourfault.weapon.General.Projectile;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

import static com.yourfault.Main.world;

public class HolySword extends Projectile {

    public HolySword(Location StartLocation, float damage) {
        super(StartLocation, 2, damage, 1f, false, 5);
        // store the starting age set by the super constructor

    }
    // initial lifetime preserved so we can compute elapsed ticks from inherited `age`

    @Override
    public void ChildUpdate()
    {
        Color color = Color.fromRGB(255,255,Math.clamp(125+(int)age* 20L,0,255));
        Particle.DustOptions dust = new Particle.DustOptions(color, 0.5f + age*0.2f);

        world.spawnParticle(Particle.DUST, getDisplayedLocation(), 100, radius*0.25, 0.1, radius*0.25, 0.0, dust);
        world.spawnParticle(Particle.END_ROD, getDisplayedLocation(), 1, radius, radius, radius, 0.5, null);

        
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
