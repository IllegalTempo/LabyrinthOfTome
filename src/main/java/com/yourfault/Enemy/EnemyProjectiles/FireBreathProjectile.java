package com.yourfault.Enemy.EnemyProjectiles;

import com.yourfault.Enemy.Enemy;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemStack;

public class FireBreathProjectile extends EnemyProjectile {

    public FireBreathProjectile(Location eyeLocation, Enemy owner) {
        super(eyeLocation, 0.6f, 4.0f, 1.5f, false, new ItemStack(Material.MAGMA_CREAM), 40, owner);
    }

    @Override
    protected void ChildUpdate() {
        entity.getWorld().spawnParticle(Particle.FLAME, getDisplayedLocation(), 2, 0.1, 0.1, 0.1, 0.02);
    }

    @Override
    public void Projectile_OnHit() {
        Destroy();
    }

    @Override
    public void Player_OnHit(GamePlayer p) {
        p.MINECRAFT_PLAYER.setFireTicks(60);
    }
}
