package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.PlagueDoctorEnemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Witch;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class PlagueDoctor_Type extends AbstractEnemyType {
    public PlagueDoctor_Type() {
        super("Plague Doctor", 3.2f, 130.0f, 6.0f, 3, 9, 0.7, 5, 9, 16, 28, EnemyClassification.NORMAL);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        Witch witch = location.getWorld().spawn(location, Witch.class);
        witch.setAware(true);
        witch.setPersistent(true);
        if (witch.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            witch.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.26);
        }
        EntityEquipment equipment = witch.getEquipment();
        if (equipment != null) {
            equipment.setHelmet(maskPiece());
            equipment.setChestplate(leatherPiece(Material.LEATHER_CHESTPLATE));
            equipment.setLeggings(leatherPiece(Material.LEATHER_LEGGINGS));
            equipment.setBoots(leatherPiece(Material.LEATHER_BOOTS));
            equipment.setItemInMainHand(new ItemStack(Material.AIR));
            equipment.setItemInOffHand(new ItemStack(Material.AIR));
            equipment.setHelmetDropChance(0.0f);
            equipment.setChestplateDropChance(0.0f);
            equipment.setLeggingsDropChance(0.0f);
            equipment.setBootsDropChance(0.0f);
        }
        return witch;
    }

    private ItemStack leatherPiece(Material material) {
        ItemStack stack = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) stack.getItemMeta();
        if (meta != null) {
            meta.setColor(Color.fromRGB(38, 60, 54));
            meta.setUnbreakable(true);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack maskPiece() {
        ItemStack stack = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta meta = (LeatherArmorMeta) stack.getItemMeta();
        if (meta != null) {
            meta.setColor(Color.fromRGB(15, 15, 15));
            meta.setUnbreakable(true);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new PlagueDoctorEnemy(e, context, this);
    }
}
