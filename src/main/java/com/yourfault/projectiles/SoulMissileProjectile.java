package com.yourfault.projectiles;

import com.yourfault.Enemy.Enemy;
import com.yourfault.system.LabyrinthCreature;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class SoulMissileProjectile extends Projectile {

    private Player target;

    public SoulMissileProjectile(Location eyeLocation, Enemy owner, Player target) {
        super(eyeLocation, 0.6f, 8.0f, 0.5f, false, new ItemStack(Material.SOUL_CAMPFIRE), 100, owner);
        this.target = target;
    }

    @Override
    public void onHit(LabyrinthCreature p) {
        if (p.minecraftEntity != null) {
            p.minecraftEntity.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1));
        }
    }

    @Override
    protected void ChildUpdate() {
        entity.getWorld().spawnParticle(Particle.SOUL, entity.getLocation(), 1);

        if (target != null && target.isValid() && !target.isDead()) {
            Location targetLoc = target.getEyeLocation();
            Location currentLoc = entity.getLocation();
            Vector desired = targetLoc.toVector().subtract(currentLoc.toVector()).normalize();
            Vector current = entity.getLocation().getDirection();
            Vector newDir = current.add(desired.multiply(0.2)).normalize();
            Location newLoc = currentLoc.clone();
            newLoc.setDirection(newDir);
            entity.teleport(newLoc);
        }
    }
}
