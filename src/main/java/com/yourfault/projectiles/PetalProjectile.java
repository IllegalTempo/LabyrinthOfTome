package com.yourfault.projectiles;

import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

import static com.yourfault.Main.world;

public class PetalProjectile extends Projectile {

    public PetalProjectile(Location StartLocation, float damage, GamePlayer owner) {
        super(StartLocation, 2.5f, damage, 1.5f, false, 20, owner);
    }

    @Override
    public void ChildUpdate()
    {
        world.spawnParticle(Particle.CHERRY_LEAVES, getDisplayedLocation(), 3, 0.1, 0.1, 0.1, 0.01);
        world.spawnParticle(Particle.END_ROD, getDisplayedLocation(), 1, 0.05, 0.05, 0.05, 0.01);
    }
}
