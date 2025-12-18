package com.yourfault.projectiles;

import com.yourfault.Enemy.Enemy;
import com.yourfault.system.LabyrinthCreature;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MudBallProjectile extends Projectile {

    public MudBallProjectile(Location eyeLocation, Enemy owner) {
        super(eyeLocation, 1.2f, 3.0f, 1.5f, false, new ItemStack(Material.CLAY_BALL), 100, owner);

    }

    @Override
    public void onHit(LabyrinthCreature p) {
       p.minecraftEntity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0)); // Blindness for 3 seconds

    }
}
