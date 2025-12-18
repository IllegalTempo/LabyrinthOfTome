package com.yourfault.projectiles;

import com.yourfault.Enemy.Enemy;
import com.yourfault.system.LabyrinthCreature;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PhaseGuardianBolt extends Projectile {

    public PhaseGuardianBolt(Location eyeLocation, Enemy owner) {
        super(eyeLocation, 0.9f, 8.0f, 1.0f, false, new ItemStack(Material.AMETHYST_SHARD), 80, owner);
    }

    @Override
    protected void ChildUpdate() {
        entity.getWorld().spawnParticle(Particle.END_ROD, entity.getLocation(), 4, 0.1, 0.1, 0.1, 0.01);
    }

    @Override
    public void onHit(LabyrinthCreature player) {
        player.minecraftEntity.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 1, true, false, false));

    }
}
