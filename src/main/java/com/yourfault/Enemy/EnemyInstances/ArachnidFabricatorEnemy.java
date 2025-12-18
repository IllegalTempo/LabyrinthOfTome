package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.Main;
import com.yourfault.projectiles.WebRocketProjectile;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ArachnidFabricatorEnemy extends Enemy {

    private int phase = 1;
    private LivingEntity turretEntity;
    private final Random random = new Random();
    private int abilityCooldown = 0;
    private final List<Block> placedWebs = new ArrayList<>();

    public ArachnidFabricatorEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
        super(entity, context, 5L, type);
    }

    @Override
    public void update() {
        if (entity.isDead()) {
            cleanup();
            return;
        }

        float hpPercent = HEALTH / MAX_HEALTH;

        if (phase == 1 && hpPercent <= 0.70) {
            startPhase2();
        } else if (phase == 2 && hpPercent <= 0.30) {
            startPhase3();
        }

        if (phase == 1) {
            phase1Logic();
        } else if (phase == 2) {
            phase2Logic();
        } else if (phase == 3) {
            phase3Logic();
        }

        if (phase == 2 && turretEntity != null && !turretEntity.isDead()) {
            if (random.nextInt(100) < 5) {
                spawnSpiderling(turretEntity.getLocation());
            }
        }
    }

    private void phase1Logic() {
        if (abilityCooldown > 0) {
            abilityCooldown--;
            return;
        }

        if (random.nextBoolean()) {
            fireWebRocket();
            abilityCooldown = 20;
        } else {
            chargeAttack();
            abilityCooldown = 30;
        }
    }

    private void phase2Logic() {
        if (abilityCooldown > 0) {
            abilityCooldown--;
            return;
        }
        if (random.nextInt(10) < 3) {
            chargeAttack();
            abilityCooldown = 20;
        }
    }

    private void phase3Logic() {
        if (abilityCooldown > 0) {
            abilityCooldown--;
            return;
        }

        if (random.nextInt(10) < 4) {
            startWeavingSequence();
            abilityCooldown = 60;
        } else {
            fireWebRocket();
            abilityCooldown = 15;
        }
    }

    private void startPhase2() {
        phase = 2;
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 2.0f, 0.5f);
        entity.getWorld().spawnParticle(Particle.EXPLOSION, entity.getLocation(), 10);
        turretEntity = (LivingEntity) entity.getWorld().spawnEntity(entity.getLocation(), EntityType.CAVE_SPIDER);
        turretEntity.setAI(false);
        turretEntity.setCustomName("§cAbdomen Turret");
        turretEntity.setCustomNameVisible(true);
        turretEntity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, 2));
        entity.getAttribute(Attribute.valueOf("GENERIC_MOVEMENT_SPEED")).setBaseValue(0.4); // Fast
        entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 1));
        org.bukkit.Bukkit.broadcastMessage("§cArachnid Fabricator detached its abdomen!");
    }

    private void startPhase3() {
        phase = 3;
        if (turretEntity != null) {
            turretEntity.remove();
            turretEntity = null;
        }
        
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 2.0f, 0.5f);
        org.bukkit.Bukkit.broadcastMessage("§cArachnid Fabricator re-attached! ELECTRIFIED WEBS!");
    }

    private void fireWebRocket() {
        if (entity.getTarget() == null) return;
        Location spawnLoc = entity.getEyeLocation();
        Vector direction = entity.getTarget().getLocation().toVector().subtract(entity.getLocation().toVector()).normalize();
        spawnLoc.setDirection(direction);
        new WebRocketProjectile(spawnLoc, this);
    }

    private void createWeb(Location loc) {
        Block b = loc.getBlock();
        if (b.getType() == Material.AIR) {
            b.setType(Material.COBWEB);
            placedWebs.add(b);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (b.getType() == Material.COBWEB) {
                        b.setType(Material.AIR);
                        placedWebs.remove(b);
                    }
                }
            }.runTaskLater(Main.plugin, 200L);
        }
    }

    private void chargeAttack() {
        if (entity.getTarget() == null) return;
        Vector dir = entity.getTarget().getLocation().toVector().subtract(entity.getLocation().toVector()).normalize();
        entity.setVelocity(dir.multiply(2.0).setY(0.5));
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_HORSE_GALLOP, 1.0f, 0.5f);
    }

    private void spawnSpiderling(Location loc) {
        Silverfish s = (Silverfish) loc.getWorld().spawnEntity(loc, EntityType.SILVERFISH);
        s.setCustomName("§7Mechanical Spiderling");
    }

    private void startWeavingSequence() {
        if (entity.getTarget() == null) return;
        Location center = entity.getTarget().getLocation();
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (Math.abs(x) == 3 || Math.abs(z) == 3) {
                    Location webLoc = center.clone().add(x, 0, z);
                    createWeb(webLoc);
                    createWeb(webLoc.add(0, 1, 0));
                }
            }
        }
        entity.getWorld().playSound(center, Sound.BLOCK_WOOL_PLACE, 2.0f, 0.5f);
    }

    private void cleanup() {
        if (turretEntity != null) turretEntity.remove();
        for (Block b : placedWebs) {
            if (b.getType() == Material.COBWEB) {
                b.setType(Material.AIR);
            }
        }
        placedWebs.clear();
    }
    
    @Override
    public void Destroy() {
        cleanup();
        super.Destroy();
    }

    @Override
    public void tick() {
        if (phase == 3) {
            for (Block b : placedWebs) {
                if (b.getType() == Material.COBWEB) {
                    b.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, b.getLocation().add(0.5, 0.5, 0.5), 1);
                    for (Entity e : b.getWorld().getNearbyEntities(b.getLocation().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5)) {
                        if (e instanceof Player) {
                            ((Player) e).damage(1.0, entity);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void OnAttack() {
    }

    @Override
    public void OnDealDamage() {
    }
}
