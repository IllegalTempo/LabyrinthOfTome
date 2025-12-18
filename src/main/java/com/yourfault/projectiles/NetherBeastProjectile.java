package com.yourfault.projectiles;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.system.LabyrinthCreature;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class NetherBeastProjectile extends Projectile {

    public NetherBeastProjectile(Location eyeLocation, Enemy owner) {
        super(eyeLocation, 1.2f, 0f, 1.5f, false, new ItemStack(Material.ORANGE_TERRACOTTA), 100, owner);
    }

    @Override
    protected void ChildUpdate() {
        if (getDisplayedLocation().getBlock().getType().isSolid()) {
            explode();
            Destroy();
        }
        entity.getWorld().spawnParticle(Particle.FLAME, getDisplayedLocation(), 2, 0.1, 0.1, 0.1, 0.05);
    }

    @Override
    public void onHit(LabyrinthCreature p) {
        explode();
        Destroy();
    }

    private void explode() {
        Location loc = getDisplayedLocation();
        loc.getWorld().createExplosion(loc, 0F, false);
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

        for (LabyrinthCreature c: Main.game.findNearbyCreature(loc,3,3,3,owner.team)) {

            c.applyDamage(8.0f, owner, false); //todo check im not sure the bypass chain


        }
    }
}
