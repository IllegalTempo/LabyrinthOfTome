package com.yourfault.projectiles;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.system.LabyrinthCreature;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class WebRocketProjectile extends Projectile {

    public WebRocketProjectile(Location eyeLocation, Enemy owner) {
        super(eyeLocation, 0.8f, 10.0f, 1.0f, false, new ItemStack(Material.COBWEB), 100, owner);
    }

    @Override
    public void Projectile_OnHit() {
        createWeb(getDisplayedLocation());
        Destroy();
    }

    private void createWeb(Location loc) {
        Block b = loc.getBlock();
        if (b.getType() == Material.AIR) {
            b.setType(Material.COBWEB);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (b.getType() == Material.COBWEB) {
                        b.setType(Material.AIR);
                    }
                }
            }.runTaskLater(Main.plugin, 100L);
        }
    }
}
