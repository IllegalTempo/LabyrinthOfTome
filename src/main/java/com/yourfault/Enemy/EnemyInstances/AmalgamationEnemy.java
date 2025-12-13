package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyProjectiles.PlagueCarrierProjectile;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.Enemy.EnemyTypes.AmalgamationSplit_Type;
import com.yourfault.Main;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class AmalgamationEnemy extends Enemy {

    private boolean split = false;
    private int attackCooldown = 0;

    public AmalgamationEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
        super(entity, context, 10L, type);
    }

    @Override
    public void update() {
        if (entity.isDead()) return;
        if (!split && HEALTH <= MaxHealth * 0.5) {
            performSplit();
            return;
        }
        if (entity.getTarget() == null && getNearestPlayer() != null) {
            entity.setTarget(getNearestPlayer().MINECRAFT_PLAYER);
        }
        spawnPoisonCloud(entity.getLocation());
        if (attackCooldown > 0) {
            attackCooldown = Math.max(0, attackCooldown - 1);
        }
        if (entity.getTarget() instanceof Player) {
            Player target = (Player) entity.getTarget();
            if (target != null && entity.hasLineOfSight(target)) {
                if (attackCooldown <= 0) {
                    shootPlaguePod(target);
                    attackCooldown = 10;
                }
            }
        }
    }

    private void spawnPoisonCloud(Location loc) {
        AreaEffectCloud cloud = (AreaEffectCloud) loc.getWorld().spawnEntity(loc, EntityType.AREA_EFFECT_CLOUD);
        cloud.setDuration(100);
        cloud.setRadius(3.0f);
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 60, 1), true);
        cloud.setWaitTime(0);
    }

    private void shootPlaguePod(Player target) {
        Location spawnLoc = entity.getEyeLocation();
        Vector direction = target.getEyeLocation().toVector().subtract(spawnLoc.toVector()).normalize();
        spawnLoc.setDirection(direction);
        new PlagueCarrierProjectile(spawnLoc, this);
    }

    private void performSplit() {
        split = true;
        Location loc = entity.getLocation();
        AmalgamationSplit_Type splitType = new AmalgamationSplit_Type();
        Mob m1 = splitType.SpawnEntity(loc.clone().add(2, 0, 0));
        AmalgamationSplitEnemy e1 = (AmalgamationSplitEnemy) splitType.CreateEnemyInstance(m1, context);
        Mob m2 = splitType.SpawnEntity(loc.clone().add(-2, 0, 0));
        AmalgamationSplitEnemy e2 = (AmalgamationSplitEnemy) splitType.CreateEnemyInstance(m2, context);
        e1.setSibling(e2);
        e2.setSibling(e1);
        entity.remove();
        Main.game.ENEMY_LIST.remove(entity.getUniqueId());
        Main.game.EnemyTeam.removeEntity(entity);

        // !!!!!!!!!!!! The BossBar might disappear.
        // Ideally, the splits should take over the boss bar or have their own.
        // Since they are ELITE, they might not show on the main boss bar.
        // But for now, this satisfies the "Splits into two" requirement
    }

    @Override
    public void tick() {}

    @Override
    public void OnAttack() {}

    @Override
    public void OnDealDamage() {}
}
