package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.ChainLasherEnemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;

public class ChainLasher_Type extends AbstractEnemyType {

    public ChainLasher_Type() {
        super("Chain Lasher", 10, 50, 8, 2, 6, 1.0, 8, 15, 25, 60, EnemyClassification.ELITE);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        Zombie zombie = (Zombie) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
        zombie.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_CHAIN));
        zombie.getEquipment().setItemInMainHandDropChance(0f);
        return zombie;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new ChainLasherEnemy(e, context, this);
    }
}
