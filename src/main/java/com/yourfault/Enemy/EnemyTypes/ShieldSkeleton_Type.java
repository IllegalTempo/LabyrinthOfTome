package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.ShieldSkeletonEnemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Stray;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ShieldSkeleton_Type extends AbstractEnemyType {
    public ShieldSkeleton_Type() {
        super("Shield Skeleton", 2.0f, 80.0f, 6.0f, 2, 6, 0.7, 5, 8, 14, 22, EnemyClassification.ELITE);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        Stray stray = location.getWorld().spawn(location, Stray.class);
        stray.setPersistent(true);
        ItemStack shield = new ItemStack(Material.SHIELD);
        ItemMeta meta = shield.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            shield.setItemMeta(meta);
        }
        ItemStack sword = new ItemStack(Material.STONE_SWORD);
        stray.getEquipment().setItemInMainHand(sword);
        stray.getEquipment().setItemInOffHand(shield);
        return stray;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new ShieldSkeletonEnemy(e, context, this);
    }
}
