package com.yourfault.Enemy.EnemyProjectiles;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collection;

public class ChainProjectile extends EnemyProjectile {

    public ChainProjectile(Location eyeLocation, Enemy owner) {
        super(eyeLocation, 1.5f, 5.0f, 1.5f, false, new ItemStack(Material.IRON_CHAIN), 60, owner);
    }

    @Override
    public void Update() {
        UpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                ChildUpdate();
                if (age == LastFor) {
                    Destroy();
                    return;
                }

                // Movement
                Vector travel = entity.getLocation().getDirection().multiply(speed);
                age += 1;
                if (UseGravity) travel.add(Main.game.Gravity);
                entity.teleport(entity.getLocation().add(travel));

                entity.getWorld().spawnParticle(Particle.CRIT, entity.getLocation(), 1);

                if (entity.getLocation().getBlock().getType().isSolid()) {
                    pullOwnerToLocation(entity.getLocation());
                    Destroy();
                    return;
                }

                Collection<? extends Player> online = Bukkit.getOnlinePlayers();
                boolean hit = false;
                for (Player p : online) {
                    if (p.getWorld() != entity.getWorld()) continue;
                    // Check distance to player's eye or center
                    if (p.getLocation().add(0, 1, 0).distanceSquared(getDisplayedLocation()) <= radius * radius) {
                        hit = true;
                        GamePlayer pl = Main.game.GetPlayer(p);
                        pl.damage(damage);
                        Player_OnHit(pl);
                    }
                }

                if (hit) Projectile_OnHit();
            }
        };
        UpdateTask.runTaskTimer(Main.plugin, 0L, 1L);
    }

    @Override
    public void Player_OnHit(GamePlayer p) {
        if (p.MINECRAFT_PLAYER != null && owner.entity != null) {
            Location ownerLoc = owner.entity.getLocation();
            Location playerLoc = p.MINECRAFT_PLAYER.getLocation();
            Vector direction = ownerLoc.toVector().subtract(playerLoc.toVector()).normalize();
            p.MINECRAFT_PLAYER.setVelocity(direction.multiply(1.5));
        }
    }

    private void pullOwnerToLocation(Location targetLoc) {
        if (owner.entity != null) {
            Location ownerLoc = owner.entity.getLocation();
            Vector direction = targetLoc.toVector().subtract(ownerLoc.toVector()).normalize();
            owner.entity.setVelocity(direction.multiply(1.5));
        }
    }
}
