package com.yourfault.projectiles;

import com.yourfault.Enemy.Enemy;
import com.yourfault.system.LabyrinthCreature;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PoisonSpitProjectile extends Projectile {

    public PoisonSpitProjectile(Location eyeLocation, Enemy owner) {
        super(eyeLocation, 1.0f, 5.0f, 1.5f, true, new ItemStack(Material.SLIME_BALL), 60, owner);
    }

    @Override
    public void Projectile_OnHit() {
        spawnPoisonCloud(getDisplayedLocation());
        Destroy();
    }

    @Override
    public void onHit(LabyrinthCreature p) {
            p.minecraftEntity.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 1));

    }

    private void spawnPoisonCloud(Location loc) {
        AreaEffectCloud cloud = (AreaEffectCloud) loc.getWorld().spawnEntity(loc, EntityType.AREA_EFFECT_CLOUD);
        cloud.setParticle(Particle.ITEM, new ItemStack(Material.SLIME_BALL));
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 100, 1), true);
        cloud.setRadius(3.0f);
        cloud.setDuration(100);
    }
}
