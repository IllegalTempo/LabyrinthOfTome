package com.yourfault.Enemy.EnemyProjectiles;

import com.yourfault.Enemy.Enemy;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlagueCarrierProjectile extends EnemyProjectile {

    public PlagueCarrierProjectile(Location eyeLocation, Enemy owner) {
        super(eyeLocation, 0.8f, 4.0f, 1.0f, false, new ItemStack(Material.SLIME_BALL), 80, owner);
    }

    @Override
    public void Player_OnHit(GamePlayer p) {
        if (p.MINECRAFT_PLAYER != null) {
            p.MINECRAFT_PLAYER.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1)); // Poison II for 5s
            p.MINECRAFT_PLAYER.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0));
        }
    }

    @Override
    protected void ChildUpdate() {
        Particle.DustOptions dust = new Particle.DustOptions(Color.GREEN, 0.6f);
        entity.getWorld().spawnParticle(Particle.DUST, entity.getLocation(), 2, 0.2, 0.2, 0.2, 0.0, dust);
    }
}
