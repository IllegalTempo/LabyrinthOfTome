package com.yourfault.Enemy.EnemyProjectiles;

import com.yourfault.Enemy.Enemy;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public class StoneclawProjectile extends EnemyProjectile {

    public StoneclawProjectile(Location eyeLocation, Enemy owner) {
        super(eyeLocation, 1.0f, 8.0f, 1.0f, false, new ItemStack(Material.COBBLESTONE), 60, owner);
    }

    @Override
    protected void ChildUpdate() {
        if (getDisplayedLocation().getBlock().getType().isSolid()) {
            entity.getWorld().playSound(getDisplayedLocation(), Sound.BLOCK_STONE_BREAK, 1f, 1f);
            entity.getWorld().spawnParticle(Particle.BLOCK, getDisplayedLocation(), 10, 0.2, 0.2, 0.2, Material.COBBLESTONE.createBlockData());
            Destroy();
        }
    }

    @Override
    public void Player_OnHit(GamePlayer p) {
        // kb
        p.getMinecraftPlayer().setVelocity(entity.getVelocity().normalize().multiply(0.8).setY(0.3));
        p.getMinecraftPlayer().getWorld().playSound(p.getMinecraftPlayer().getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 1f);
        Destroy();
    }
}
