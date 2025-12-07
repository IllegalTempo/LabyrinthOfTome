package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.GeneralEnemyInstance;
import com.yourfault.Enemy.EnemyInstances.SpinnyEnemy;
import com.yourfault.Main;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.List;

public class Spinny_Type extends AbstractEnemyType {
    public Spinny_Type() {
        super("Spinny", 2, 15f, 5f, 1, 2, 1, 5, 10, 20, 30, EnemyClassification.NORMAL);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        return location.getWorld().spawn(location, Zombie.class, e -> {
            e.setCustomNameVisible(true);
            e.setAI(true);
            e.setInvisible(true);
            ItemStack result = new ItemStack(Material.IRON_SWORD);
            ItemMeta meta = result.getItemMeta();

            meta.setUnbreakable(true);
            CustomModelDataComponent component = meta.getCustomModelDataComponent();
            component.setStrings(List.of("spinny_1", String.valueOf(((Main.world.getGameTime() % 24000) - 0) % 20)));
            meta.setCustomModelDataComponent(component);

            result.setItemMeta(meta);
            e.getEquipment().setHelmet(result);

            // Additional customization can be added here

        });
    }
    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context)
    {
        return new SpinnyEnemy(e,context,this);
    }
}
