package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.Main;
import com.yourfault.wave.WaveContext;
import org.bukkit.Particle;
import org.bukkit.entity.Mob;
import org.bukkit.scheduler.BukkitRunnable;

public class AmalgamationSplitEnemy extends Enemy {

    private AmalgamationSplitEnemy sibling;
    private boolean absorbing = false;
    private int absorptionTimer = 0;

    public AmalgamationSplitEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
        super(entity, context, 20L, type);
    }

    public void setSibling(AmalgamationSplitEnemy sibling) {
        this.sibling = sibling;
    }

    @Override
    public void update() {
        if (entity.isDead()) return;

        if (sibling != null) {
            if (sibling.entity.isDead() || !sibling.entity.isValid()) {
                if (!absorbing) {
                    startAbsorption();
                }
            }
        }

        if (absorbing) {
            absorptionTimer++;
            entity.getWorld().spawnParticle(Particle.HEART, entity.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5);

            if (absorptionTimer >= 15) {
                finishAbsorption();
            }
        }
        if (entity.getTarget() == null && getNearestPlayer() != null) {
            entity.setTarget(getNearestPlayer().MINECRAFT_PLAYER);
        }
    }

    private void startAbsorption() {
        absorbing = true;
        absorptionTimer = 0;
    }

    private void finishAbsorption() {
        absorbing = false;
        float healAmount = MAX_HEALTH * 0.25f;
        HEALTH = Math.min(MAX_HEALTH, HEALTH + healAmount);
        updateDisplay();
        entity.getWorld().spawnParticle(Particle.HEART, entity.getLocation().add(0, 2, 0), 10);
        sibling = null;
    }

    @Override
    public void tick() {}

    @Override
    public void OnAttack() {}

    @Override
    public void OnDealDamage() {}
}
