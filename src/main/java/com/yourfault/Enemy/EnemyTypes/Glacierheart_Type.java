package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.GlacierheartEnemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Stray;
import org.bukkit.inventory.ItemStack;

public class Glacierheart_Type extends AbstractEnemyType {

    public Glacierheart_Type() {
        super("The Glacierheart", 1, 500, 12, 4, 20, 0, 150, 300, 600, 1200, EnemyClassification.BOSS, true);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        Stray stray = (Stray) location.getWorld().spawnEntity(location, EntityType.STRAY);
        stray.getEquipment().setHelmet(new ItemStack(Material.ICE));
        stray.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        return stray;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new GlacierheartEnemy(e, context, this);
    }
}
