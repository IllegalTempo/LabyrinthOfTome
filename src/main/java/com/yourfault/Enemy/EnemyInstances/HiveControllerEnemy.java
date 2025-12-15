package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.Main;
import com.yourfault.wave.WaveContext;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class HiveControllerEnemy extends Enemy {

    private final List<UUID> controlledEnemies = new ArrayList<>();
    private static final int MAX_CONTROLLED = 2;
    private static final double CONTROL_RANGE = 15.0;

    public HiveControllerEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
        super(entity, context, 20L, type);
    }

    @Override
    public void update() {
        if (entity.isDead()) {
            releaseAll();
            return;
        }
        Iterator<UUID> it = controlledEnemies.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            Entity e = Main.plugin.getServer().getEntity(id);
            if (e == null || e.isDead() || e.getLocation().distance(entity.getLocation()) > CONTROL_RANGE * 1.5) {
                if (e instanceof LivingEntity) {
                    resetBuffs((LivingEntity) e);
                }
                it.remove();
            } else {
                drawLink(e.getLocation());
            }
        }
        if (controlledEnemies.size() < MAX_CONTROLLED) {
            List<Entity> nearby = entity.getNearbyEntities(CONTROL_RANGE, CONTROL_RANGE, CONTROL_RANGE);
            for (Entity e : nearby) {
                if (controlledEnemies.size() >= MAX_CONTROLLED) break;
                if (e instanceof Mob && !e.getUniqueId().equals(entity.getUniqueId())) {
                    Enemy enemyInstance = Main.game.ENEMY_LIST.get(e.getUniqueId());
                    if (enemyInstance != null && enemyInstance.enemyType.classification == EnemyClassification.NORMAL && !controlledEnemies.contains(e.getUniqueId())) {
                        controlledEnemies.add(e.getUniqueId());
                        applyBuffs((LivingEntity) e);
                    }
                }
            }
        }
    }

    private void applyBuffs(LivingEntity e) {
        if (e.getAttribute(Attribute.valueOf("GENERIC_ATTACK_DAMAGE")) != null) {
            double base = e.getAttribute(Attribute.valueOf("GENERIC_ATTACK_DAMAGE")).getBaseValue();
            e.getAttribute(Attribute.valueOf("GENERIC_ATTACK_DAMAGE")).setBaseValue(base * 2.0);
        }
        if (e.getAttribute(Attribute.valueOf("GENERIC_MAX_HEALTH")) != null) {
            double base = e.getAttribute(Attribute.valueOf("GENERIC_MAX_HEALTH")).getBaseValue();
            e.getAttribute(Attribute.valueOf("GENERIC_MAX_HEALTH")).setBaseValue(base * 2.0);
            e.setHealth(e.getAttribute(Attribute.valueOf("GENERIC_MAX_HEALTH")).getValue()); // Heal to new max
        }
        e.setGlowing(true);
    }

    private void resetBuffs(LivingEntity e) {
        if (e.getAttribute(Attribute.valueOf("GENERIC_ATTACK_DAMAGE")) != null) {
            double base = e.getAttribute(Attribute.valueOf("GENERIC_ATTACK_DAMAGE")).getBaseValue();
            e.getAttribute(Attribute.valueOf("GENERIC_ATTACK_DAMAGE")).setBaseValue(base / 2.0);
        }
        if (e.getAttribute(Attribute.valueOf("GENERIC_MAX_HEALTH")) != null) {
            double base = e.getAttribute(Attribute.valueOf("GENERIC_MAX_HEALTH")).getBaseValue();
            e.getAttribute(Attribute.valueOf("GENERIC_MAX_HEALTH")).setBaseValue(base / 2.0);
        }
        e.setGlowing(false);
    }

    private void releaseAll() {
        for (UUID id : controlledEnemies) {
            Entity e = Main.plugin.getServer().getEntity(id);
            if (e instanceof LivingEntity) {
                resetBuffs((LivingEntity) e);
            }
        }
        controlledEnemies.clear();
    }
    
    @Override
    public void Destroy() {
        releaseAll();
        super.Destroy();
    }

    private void drawLink(Location targetLoc) {
        Location start = entity.getLocation().add(0, 0.5, 0);
        Location end = targetLoc.add(0, 1.0, 0);
        Vector direction = end.toVector().subtract(start.toVector());
        double distance = start.distance(end);
        direction.normalize();

        for (double d = 0; d < distance; d += 0.5) {
            Location point = start.clone().add(direction.clone().multiply(d));
            entity.getWorld().spawnParticle(Particle.DUST, point, 1, new Particle.DustOptions(Color.RED, 1));
        }
    }

    @Override
    public void tick() {
    }

    @Override
    public void OnAttack() {
    }

    @Override
    public void OnDealDamage() {
    }
}
