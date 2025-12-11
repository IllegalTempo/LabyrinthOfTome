package com.yourfault.Enemy.EnemyProjectiles;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class NetherBeastProjectile extends EnemyProjectile {

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
    public void Player_OnHit(GamePlayer p) {
        explode();
        Destroy();
    }

    private void explode() {
        Location loc = getDisplayedLocation();
        loc.getWorld().createExplosion(loc, 0F, false);
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

        for (Entity e: loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
            if (e instanceof Player) {
                GamePlayer gp = Main.game.GetPlayer((Player) e);
                if (gp != null) {
                    gp.damage(8.0f);
                }
            }
        }
    }
}
