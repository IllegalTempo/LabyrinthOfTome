package com.yourfault.weapon.Excalibur;

import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.system.Projectile;
import org.bukkit.*;

import static com.yourfault.Main.world;

public class Sword_Aura extends Projectile {

    public Sword_Aura(Location StartLocation, float damage, GamePlayer owner) {
        super(StartLocation, 2, damage, 1f, false, 5,owner);
    }

    @Override
    public void ChildUpdate()
    {
        Color color = Color.fromRGB(255,255,Math.clamp(125+(int)age* 20L,0,255));
        Particle.DustOptions dust = new Particle.DustOptions(color, 0.5f + age*0.2f);

        world.spawnParticle(Particle.DUST, getDisplayedLocation(), (int) (100*radius), radius*0.25, radius*0.25, radius*0.25, 0.0, dust);
        world.spawnParticle(Particle.END_ROD, getDisplayedLocation(), (int) (1*radius), radius, radius, radius, 0.5, null);


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
