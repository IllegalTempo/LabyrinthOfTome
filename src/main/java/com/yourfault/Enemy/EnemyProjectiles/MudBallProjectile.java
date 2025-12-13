package com.yourfault.Enemy.EnemyProjectiles;

import com.yourfault.Enemy.Enemy;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MudBallProjectile extends EnemyProjectile {

    public MudBallProjectile(Location eyeLocation, Enemy owner) {
        super(eyeLocation, 1.2f, 3.0f, 1.5f, false, new ItemStack(Material.CLAY_BALL), 100, owner);

    }

    @Override
    public void Player_OnHit(GamePlayer p) {
        if (p.MINECRAFT_PLAYER != null) {
            p.MINECRAFT_PLAYER.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0)); // Blindness for 3 seconds
        }
    }
}
