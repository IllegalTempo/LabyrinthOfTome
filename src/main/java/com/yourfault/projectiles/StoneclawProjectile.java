package com.yourfault.projectiles;

import com.yourfault.Enemy.Enemy;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.system.LabyrinthCreature;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class StoneclawProjectile extends Projectile {

    public StoneclawProjectile(Location eyeLocation, Enemy owner) {
        // speed 1.2, damage 8, radius 0.8, gravity true, item COBBLESTONE, lastFor 60 ticks
        super(eyeLocation, 1.2f, 8.0f, 0.8f, true, new ItemStack(Material.COBBLESTONE), 60, owner);
    }

    @Override
    protected void ChildUpdate() {
        if (getDisplayedLocation().getBlock().getType().isSolid()) {
            entity.getWorld().playSound(getDisplayedLocation(), Sound.BLOCK_STONE_BREAK, 1f, 0.5f);
            entity.getWorld().spawnParticle(Particle.BLOCK, getDisplayedLocation(), 10, 0.2, 0.2, 0.2, Material.COBBLESTONE.createBlockData());
            Destroy();
        }
        // Particles trail
        entity.getWorld().spawnParticle(Particle.CRIT, getDisplayedLocation(), 1, 0, 0, 0, 0);
    }

    @Override
    public void onHit(LabyrinthCreature p) {
        // Apply knockback
        Vector knockback = p.minecraftEntity.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize().multiply(1.5).setY(0.5);
        p.minecraftEntity.setVelocity(knockback);

        p.minecraftEntity.getWorld().playSound(p.minecraftEntity.getLocation(), Sound.ENTITY_IRON_GOLEM_DAMAGE, 1f, 0.5f);
        Destroy();
    }
}
