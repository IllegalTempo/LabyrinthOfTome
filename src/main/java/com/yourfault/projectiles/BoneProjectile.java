package com.yourfault.projectiles;

import com.yourfault.Enemy.Enemy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class BoneProjectile extends Projectile {

    public BoneProjectile(Location eyeLocation, Enemy owner) {
        super(eyeLocation, 1.8f, 2.0f, 1.0f, false, new ItemStack(Material.BONE), 100, owner);

    }

}
