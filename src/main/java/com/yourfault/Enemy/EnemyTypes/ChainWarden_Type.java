package com.yourfault.Enemy.EnemyTypes;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.inventory.ItemStack;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.ChainWardenEnemy;
import com.yourfault.wave.WaveContext;

public class ChainWarden_Type extends AbstractEnemyType {

    public ChainWarden_Type() {
        super("Chain Warden", 15, 70, 12, 3, 10, 1.0, 15, 30, 40, 80, EnemyClassification.ELITE);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        WitherSkeleton ws = (WitherSkeleton) location.getWorld().spawnEntity(location, EntityType.WITHER_SKELETON);
        ws.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD)); // Or chains?
        return ws;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new ChainWardenEnemy(e, context, this);
    }
}
