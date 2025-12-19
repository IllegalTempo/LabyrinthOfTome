package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.BloodKnightEnemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BloodKnight_Type extends AbstractEnemyType {
    public BloodKnight_Type() {
        super("Blood Knight", 4.5f, 180.0f, 11.0f, 3, 10, 0.65, 6, 10, 18, 30, EnemyClassification.NORMAL);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        Zombie zombie = location.getWorld().spawn(location, Zombie.class);
        zombie.setShouldBurnInDay(false);
        zombie.setBaby(false);
        zombie.setPersistent(true);
        zombie.setCanBreakDoors(true);
        if (zombie.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            zombie.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.25);
        }
        EntityEquipment equipment = zombie.getEquipment();
        if (equipment != null) {
            equipment.setHelmet(unbreakable(Material.NETHERITE_HELMET));
            equipment.setChestplate(unbreakable(Material.NETHERITE_CHESTPLATE));
            equipment.setLeggings(unbreakable(Material.NETHERITE_LEGGINGS));
            equipment.setBoots(unbreakable(Material.NETHERITE_BOOTS));
            equipment.setItemInMainHand(unbreakable(Material.NETHERITE_AXE));
            equipment.setItemInOffHand(unbreakable(Material.SHIELD));
            equipment.setHelmetDropChance(0.0f);
            equipment.setChestplateDropChance(0.0f);
            equipment.setLeggingsDropChance(0.0f);
            equipment.setBootsDropChance(0.0f);
            equipment.setItemInMainHandDropChance(0.0f);
            equipment.setItemInOffHandDropChance(0.0f);
        }
        return zombie;
    }

    private ItemStack unbreakable(Material material) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new BloodKnightEnemy(e, context, this);
    }
}
