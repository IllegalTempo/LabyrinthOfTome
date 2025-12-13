package com.yourfault.Enemy.EnemyProjectiles;

import com.yourfault.Enemy.Enemy;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class IceShardProjectile extends EnemyProjectile {
    public IceShardProjectile(Location eyeLocation, Enemy owner) {
        super(eyeLocation, 1.2f, 5.0f, 0.8f, false, new ItemStack(Material.PRISMARINE_SHARD), 80, owner);
    }

    @Override
    public void Player_OnHit(GamePlayer p) {
        createSlowingZone(p.MINECRAFT_PLAYER.getLocation());
    }

    @Override
    public void Projectile_OnHit() {
        createSlowingZone(getDisplayedLocation());
    }

    private void createSlowingZone(Location loc) {
        AreaEffectCloud cloud = (AreaEffectCloud) loc.getWorld().spawnEntity(loc, EntityType.AREA_EFFECT_CLOUD);
        cloud.setDuration(100);
        cloud.setRadius(4.0f);
        cloud.setParticle(Particle.SNOWFLAKE);
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2), true);
        cloud.setWaitTime(0);
    }

    @Override
    protected void ChildUpdate() {
        entity.getWorld().spawnParticle(Particle.SNOWFLAKE, entity.getLocation(), 1);
    }
}
