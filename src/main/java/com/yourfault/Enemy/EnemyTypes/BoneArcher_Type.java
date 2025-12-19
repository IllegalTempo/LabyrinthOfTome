package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.BoneArcherEnemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Skeleton;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BoneArcher_Type extends AbstractEnemyType {
    public BoneArcher_Type() {
        super("Bone Archer", 1.5f, 45.0f, 9.0f, 2, 6, 0.8, 4, 6, 10, 18, EnemyClassification.ELITE);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        Skeleton skeleton = location.getWorld().spawn(location, Skeleton.class);
        skeleton.setPersistent(true);
        skeleton.getEquipment().setItemInMainHand(createBow());
        skeleton.getEquipment().setItemInOffHand(null);
        return skeleton;
    }

    public ItemStack createBow() {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            bow.setItemMeta(meta);
        }
        return bow;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new BoneArcherEnemy(e, context, this);
    }
}
