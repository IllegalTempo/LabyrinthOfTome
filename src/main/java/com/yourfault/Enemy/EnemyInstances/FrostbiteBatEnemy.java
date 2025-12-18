package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class FrostbiteBatEnemy extends Enemy {
    private int attackCooldown = 0;

    public FrostbiteBatEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
        super(entity, context, type);
    }

    @Override
    public void update() {}

    @Override
    public void tick() {
        GamePlayer gp = getNearestPlayer();
        if (gp == null || gp.MINECRAFT_PLAYER == null) return;
        Player target = gp.MINECRAFT_PLAYER;

        Location targetLoc = target.getEyeLocation();
        Vector dir = targetLoc.toVector().subtract(entity.getLocation().toVector()).normalize();
        entity.setVelocity(dir.multiply(0.5));

        if (attackCooldown > 0) {
            attackCooldown--;
        }

        if (entity.getLocation().distance(targetLoc) < 1.5) {
             if (attackCooldown <= 0) {
                 gp.applyDamage(enemyType.baseDamage,this,false);//todo check
                 gp.applySpeedBoost(-30, 60);
                 attackCooldown = 20;
             }
        }
    }

    @Override
    public void OnAttack() {}

    @Override
    public void OnDealDamage() {}
}
