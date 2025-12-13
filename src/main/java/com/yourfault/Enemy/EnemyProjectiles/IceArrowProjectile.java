package com.yourfault.Enemy.EnemyProjectiles;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class IceArrowProjectile extends EnemyProjectile {

    public IceArrowProjectile(Location eyeLocation, Enemy owner) {
        super(eyeLocation, 1.5f, 6.0f, 0.5f, false, new ItemStack(Material.ARROW), 100, owner);
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
                org.bukkit.util.Vector travel = entity.getLocation().getDirection().multiply(speed);
                age += 1;
                if (UseGravity) travel.add(Main.game.Gravity);

                Location newLoc = entity.getLocation().add(travel);

                if (newLoc.getBlock().getType().isSolid()) {
                    createIcePatch(newLoc);
                    Destroy();
                    return;
                }

                entity.teleport(newLoc);

                java.util.Collection<? extends org.bukkit.entity.Player> online = org.bukkit.Bukkit.getOnlinePlayers();
                boolean hit = false;
                for (org.bukkit.entity.Player p : online) {
                    if (p.getWorld() != entity.getWorld()) continue;
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
        if (p.MINECRAFT_PLAYER != null) {
            p.applySpeedBoost(-30, 60);
        }
    }

    private void createIcePatch(Location loc) {
//        Block block = loc.getBlock();
        Block hitBlock = loc.getBlock();
        Block above = hitBlock.getRelative(0, 1, 0);

        if (hitBlock.getType().isSolid() && above.getType() == Material.AIR) {
            above.setType(Material.ICE);
            removeIceLater(above);
        } else if (hitBlock.getType() == Material.AIR) {
            hitBlock.setType(Material.ICE);
            removeIceLater(hitBlock);
        }
    }

    private void removeIceLater(Block block) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (block.getType() == Material.ICE) {
                    block.setType(Material.AIR);
                }
            }
        }.runTaskLater(Main.plugin, 100L);
    }
}
