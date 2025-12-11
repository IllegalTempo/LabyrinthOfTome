package com.yourfault.Enemy.EnemyProjectiles;

import com.yourfault.Enemy.Enemy;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public class FrostWraithProjectile extends EnemyProjectile {

    public FrostWraithProjectile(Location eyeLocation, Enemy owner) {
        super(eyeLocation, 1.5f, 5.0f, 1.0f, false, new ItemStack(Material.ICE), 100, owner);
    }

    @Override
    protected void ChildUpdate() {
        if (getDisplayedLocation().getBlock().getType().isSolid()) {
            entity.getWorld().playSound(getDisplayedLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
            Destroy();
        }
        //particle trail
        entity.getWorld().spawnParticle(Particle.SNOWFLAKE, getDisplayedLocation(), 2, 0.1, 0.1, 0.1, 0.05);
    }

    @Override
    public void Player_OnHit(GamePlayer p) {
        p.applySpeedBoost(-50f, 40); //-50 second for 2 seconds
        p.getMinecraftPlayer().getWorld().playSound(p.getMinecraftPlayer().getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
        Destroy();
    }
}
