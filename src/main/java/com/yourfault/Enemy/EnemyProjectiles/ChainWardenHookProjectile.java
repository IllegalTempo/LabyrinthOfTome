package com.yourfault.Enemy.EnemyProjectiles;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyInstances.ChainWardenEnemy;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ChainWardenHookProjectile extends EnemyProjectile {

    public ChainWardenHookProjectile(Location eyeLocation, Enemy owner) {
        super(eyeLocation, 1.2f, 5.0f, 1.5f, false, new ItemStack(Material.IRON_CHAIN), 60, owner);
    }

    @Override
    public void Update() {
        UpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                ChildUpdate();
                if (age >= LastFor) {
                    Destroy();
                    return;
                }
                Vector travel = entity.getLocation().getDirection().multiply(speed);
                age += 1;
                if (UseGravity) travel.add(Main.game.Gravity);
                entity.teleport(entity.getLocation().add(travel));
                entity.getWorld().spawnParticle(Particle.CRIT, entity.getLocation(), 1);
                if (entity.getLocation().getBlock().getType().isSolid()) {
                    Destroy();
                    return;
                }
                for (Player p : entity.getWorld().getPlayers()) {
                    if (p.getWorld() != entity.getWorld()) continue;
                    if (p.getLocation().add(0, 1, 0).distanceSquared(getDisplayedLocation()) <= radius * radius) {
                        GamePlayer pl = Main.game.GetPlayer(p);
                        if (pl != null) {
                            pl.damage(damage);
                            Player_OnHit(pl);
                            Destroy();
                            return;
                        }
                    }
                }
            }
        };
        UpdateTask.runTaskTimer(Main.plugin, 0L, 1L);
    }

    @Override
    public void Player_OnHit(GamePlayer p) {
        if (owner instanceof ChainWardenEnemy) {
            ((ChainWardenEnemy) owner).onHookHit(p.MINECRAFT_PLAYER);
        }
        p.MINECRAFT_PLAYER.playSound(p.MINECRAFT_PLAYER.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);
    }
}
