package com.yourfault.projectiles;

import com.yourfault.Enemy.Enemy;
import com.yourfault.system.LabyrinthCreature;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class ChainProjectile extends Projectile {

    public ChainProjectile(Location eyeLocation, Enemy owner) {
        super(eyeLocation, 1.5f, 5.0f, 1.5f, false, new ItemStack(Material.IRON_CHAIN), 60, owner);
    }


    @Override
    public void ChildUpdate() {
        entity.getWorld().spawnParticle(Particle.CRIT, entity.getLocation(), 1);

        if (entity.getLocation().getBlock().getType().isSolid()) {
            pullOwnerToLocation(entity.getLocation());
            Destroy();
        }
    }
    @Override
    public void onHit(LabyrinthCreature c) {
        if (c.minecraftEntity != null && owner.minecraftEntity != null) {
            Location ownerLoc = owner.minecraftEntity.getLocation();
            Location playerLoc = c.minecraftEntity.getLocation();
            Vector direction = ownerLoc.toVector().subtract(playerLoc.toVector()).normalize();
            c.minecraftEntity.setVelocity(direction.multiply(1.5));
        }
    }

    private void pullOwnerToLocation(Location targetLoc) {
        if (owner.minecraftEntity != null) {
            Location ownerLoc = owner.minecraftEntity.getLocation();
            Vector direction = targetLoc.toVector().subtract(ownerLoc.toVector()).normalize();
            owner.minecraftEntity.setVelocity(direction.multiply(1.5));
        }
    }
}
