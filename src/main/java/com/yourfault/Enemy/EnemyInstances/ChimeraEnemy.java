package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.Main;
import com.yourfault.wave.WaveContext;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Random;

public class ChimeraEnemy extends Enemy {

    private int phase = 1;
    private final Random random = new Random();
    private int abilityCooldown = 0;

    public ChimeraEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
        super(entity, context, 5L, type);
    }

    @Override
    public void update() {
        if (entity.isDead()) return;

        float hpPercent = HEALTH / MaxHealth;

        if (phase == 1 && hpPercent <= 0.66) {
            startPhase2();
        } else if (phase == 2 && hpPercent <= 0.33) {
            startPhase3();
        }

        if (abilityCooldown > 0) {
            abilityCooldown--;
            return;
        }

        if (phase == 1) {
            phase1Logic();
        } else if (phase == 2) {
            phase2Logic();
        } else if (phase == 3) {
            phase3Logic();
        }
    }

    private void phase1Logic() {
        int roll = random.nextInt(10);
        if (roll < 3) {
            chargeAttack();
            abilityCooldown = 30;
        } else if (roll < 5) {
            summonPicklers();
            abilityCooldown = 100;
        }
    }

    private void phase2Logic() {
        int roll = random.nextInt(10);
        if (roll < 3) {
            poisonSpit();
            abilityCooldown = 20;
        } else if (roll < 5) {
            chargeAttack();
            abilityCooldown = 30;
        } else if (roll < 7) {
            toxicFumes();
            abilityCooldown = 60;
        }
    }

    private void phase3Logic() {
        if (entity.isOnGround() && random.nextBoolean()) {
            entity.setVelocity(new Vector(0, 1.5, 0));
        } else if (!entity.isOnGround()) {
            if (entity.getTarget() != null) {
                Vector dir = entity.getTarget().getLocation().toVector().subtract(entity.getLocation().toVector()).normalize();
                entity.setVelocity(dir.multiply(0.8));
            }
        }

        int roll = random.nextInt(10);
        if (roll < 3) {
            fireBreath();
            abilityCooldown = 20;
        } else if (roll < 5) {
            if (random.nextBoolean()) poisonSpit(); else summonPicklers();
            abilityCooldown = 40;
        }
    }

    private void startPhase2() {
        phase = 2;
        org.bukkit.Bukkit.broadcastMessage("§6The Chimera's Goat Head emerges! Toxic fumes fill the air!");
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GOAT_SCREAMING_PREPARE_RAM, 2.0f, 0.5f);
        entity.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, entity.getLocation(), 20);
    }

    private void startPhase3() {
        phase = 3;
        org.bukkit.Bukkit.broadcastMessage("§4The Chimera's Dragon Head emerges! It takes to the skies!");
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);
        entity.setGravity(false);
    }

    private void chargeAttack() {
        if (entity.getTarget() == null) return;
        Vector dir = entity.getTarget().getLocation().toVector().subtract(entity.getLocation().toVector()).normalize();
        entity.setVelocity(dir.multiply(2.5).setY(0.5));
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.0f);
    }

    private void summonPicklers() {
        AbstractEnemyType picklerType = Main.game.getWaveManager().EnemyTypes.get("pickler");
        if (picklerType == null) return;
        for (int i = 0; i < 3; i++) {
            Location loc = entity.getLocation().add((random.nextDouble() - 0.5) * 4, 0, (random.nextDouble() - 0.5) * 4);
            Main.game.getWaveManager().spawnEnemyAt(loc, picklerType);
        }
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_INFECT, 1.0f, 1.5f);
    }

    private void poisonSpit() {
        if (entity.getTarget() == null) return;
        Location spawnLoc = entity.getEyeLocation();
        Vector direction = entity.getTarget().getLocation().toVector().subtract(entity.getLocation().toVector()).normalize();
        spawnLoc.setDirection(direction);
        new com.yourfault.Enemy.EnemyProjectiles.PoisonSpitProjectile(spawnLoc, this);
    }

    private void toxicFumes() {
        for (int i = 0; i < 5; i++) {
            Location loc = entity.getLocation().add((random.nextDouble() - 0.5) * 10, 0, (random.nextDouble() - 0.5) * 10);
            AreaEffectCloud cloud = (AreaEffectCloud) loc.getWorld().spawnEntity(loc, EntityType.AREA_EFFECT_CLOUD);
            cloud.setColor(Color.GREEN);
            cloud.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 100, 0), true);
            cloud.setRadius(4.0f);
            cloud.setDuration(200);
        }
    }

    private void fireBreath() {
        if (entity.getTarget() == null) return;
        Location spawnLoc = entity.getEyeLocation();
        Vector direction = entity.getTarget().getLocation().toVector().subtract(entity.getLocation().toVector()).normalize();
        spawnLoc.setDirection(direction);
        
        for (int i = 0; i < 3; i++) {
            Location spreadLoc = spawnLoc.clone();
            Vector spreadDir = direction.clone().add(new Vector((random.nextDouble()-0.5)*0.2, (random.nextDouble()-0.5)*0.2, (random.nextDouble()-0.5)*0.2));
            spreadLoc.setDirection(spreadDir);
            new com.yourfault.Enemy.EnemyProjectiles.FireBreathProjectile(spreadLoc, this);
        }
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);
    }

    @Override
    public void tick() {
        if (phase == 3 && !entity.isOnGround()) {
            entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation(), 5, 0.5, 0.5, 0.5, 0.05);
        }
    }

    @Override
    public void OnAttack() {
    }

    @Override
    public void OnDealDamage() {
    }
}
