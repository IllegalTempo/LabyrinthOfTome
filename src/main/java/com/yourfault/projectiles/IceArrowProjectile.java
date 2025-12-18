package com.yourfault.projectiles;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Main;
import com.yourfault.system.LabyrinthCreature;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class IceArrowProjectile extends Projectile {

    public IceArrowProjectile(Location eyeLocation, Enemy owner) {
        super(eyeLocation, 1.5f, 6.0f, 0.5f, false, new ItemStack(Material.ARROW), 100, owner);
    }
    @Override
    public void onObstacle(Location loc)
    {
        createIcePatch(loc);
    }

    @Override
    public void onHit(LabyrinthCreature p) {
        if (p.minecraftEntity != null) {
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
