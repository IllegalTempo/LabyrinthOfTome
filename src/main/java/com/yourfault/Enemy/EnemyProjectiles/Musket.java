package com.yourfault.Enemy.EnemyProjectiles;

import com.yourfault.Enemy.Enemy;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

import static com.yourfault.Main.world;

public class Musket extends EnemyProjectile {
    public Musket(Location eyeLocation, float damage, Enemy owner) {
        super(eyeLocation, 0.5f, damage, 2, false, 60, owner);
    }


    @Override
    protected void ChildUpdate() {
        Particle.DustOptions dust = new Particle.DustOptions(Color.BLACK, radius);

        world.spawnParticle(Particle.DUST, getDisplayedLocation(), 10, 0.1, 0.1, 0.1, 0.0, dust);


    }
}
