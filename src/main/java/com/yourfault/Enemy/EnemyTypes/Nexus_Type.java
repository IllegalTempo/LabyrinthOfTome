package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.NexusEnemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.inventory.ItemStack;

public class Nexus_Type extends AbstractEnemyType {

    public Nexus_Type() {
        super("Nexus, the Soul Harvester", 1, 600, 10, 5, 25, 0, 200, 400, 800, 1500, EnemyClassification.BOSS, true);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        WitherSkeleton ws = (WitherSkeleton) location.getWorld().spawnEntity(location, EntityType.WITHER_SKELETON);
        ws.getEquipment().setHelmet(new ItemStack(Material.SOUL_LANTERN));
        ws.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_HOE));
        return ws;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new NexusEnemy(e, context, this);
    }
}
