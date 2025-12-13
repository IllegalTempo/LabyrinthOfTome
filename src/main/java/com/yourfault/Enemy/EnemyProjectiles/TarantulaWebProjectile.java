package com.yourfault.Enemy.EnemyProjectiles;

import com.yourfault.Enemy.Enemy;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class TarantulaWebProjectile extends EnemyProjectile {

    public TarantulaWebProjectile(Location eyeLocation, Enemy owner) {
        super(eyeLocation, 1.5f, 2.0f, 1.5f, false, new ItemStack(Material.COBWEB), 100, owner);

    }

    @Override
    public void Player_OnHit(GamePlayer p) {
        p.applySpeedBoost(-80, 60); // -80 speed for 3 seconds (60 ticks)
    }
}
