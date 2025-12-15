package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.PicklerEnemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class Pickler_Type extends AbstractEnemyType {

    public Pickler_Type() {
        super("Pickler", 2, 15, 4, 1, 1, 1.0, 2, 5, 5, 10, EnemyClassification.NORMAL);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        Zombie z = (Zombie) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
        z.setBaby(true);
        
        ItemStack helmet = new ItemStack(Material.SEA_PICKLE);
        ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta meta = (LeatherArmorMeta) chest.getItemMeta();
        if (meta != null) {
            meta.setColor(Color.GREEN);
            chest.setItemMeta(meta);
        }
        
        ItemStack legs = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta legMeta = (LeatherArmorMeta) legs.getItemMeta();
        if (legMeta != null) {
            legMeta.setColor(Color.GREEN);
            legs.setItemMeta(legMeta);
        }

        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta bootMeta = (LeatherArmorMeta) boots.getItemMeta();
        if (bootMeta != null) {
            bootMeta.setColor(Color.GREEN);
            boots.setItemMeta(bootMeta);
        }
        
        z.getEquipment().setHelmet(helmet);
        z.getEquipment().setChestplate(chest);
        z.getEquipment().setLeggings(legs);
        z.getEquipment().setBoots(boots);
        
        return z;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new PicklerEnemy(e, context, this);
    }
}
