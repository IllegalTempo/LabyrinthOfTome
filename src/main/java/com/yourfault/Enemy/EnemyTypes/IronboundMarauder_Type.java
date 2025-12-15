package com.yourfault.Enemy.EnemyTypes;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.IronboundMarauderEnemy;
import com.yourfault.wave.WaveContext;

public class IronboundMarauder_Type extends AbstractEnemyType {

    public IronboundMarauder_Type() {
        super("Ironbound Marauder", 10, 50, 8, 3, 7, 1.0, 8, 15, 25, 50, EnemyClassification.ELITE);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        Zombie zombie = (Zombie) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
        zombie.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
        zombie.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        zombie.getEquipment().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        zombie.getEquipment().setBoots(new ItemStack(Material.IRON_BOOTS));
        zombie.getEquipment().setItemInOffHand(new ItemStack(Material.SHIELD));
        zombie.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
        return zombie;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new IronboundMarauderEnemy(e, context, this);
    }
}
