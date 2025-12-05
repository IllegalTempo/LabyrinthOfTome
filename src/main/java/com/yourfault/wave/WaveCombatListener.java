package com.yourfault.wave;

import com.yourfault.system.Game;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

public class WaveCombatListener implements Listener {
    private final Game game;

    public WaveCombatListener(Game game) {
        this.game = game;
    }

//    @EventHandler
//    public void onEnemyDamage(EntityDamageByEntityEvent event) {
//        WaveManager waveManager = game.getWaveManager();
//        if (waveManager == null || !waveManager.isActive()) {
//            return;
//        }
//        Entity target = event.getEntity();
//        if (!(target instanceof LivingEntity living)) {
//            return;
//        }
//        UUID enemyId = living.getUniqueId();
//        if (waveManager.getActiveEnemy(enemyId) == null) {
//            return;
//        }
//        GamePlayer attacker = resolveGamePlayer(event.getDamager());
//        if (attacker == null) {
//            return;
//        }
//        waveManager.handleEnemyHit(enemyId, attacker);
//    }

    @EventHandler
    public void onEnemyDeath(EntityDeathEvent event) {
        WaveManager waveManager = game.getWaveManager();
        if (waveManager == null || !waveManager.isActive()) {
            return;
        }
        UUID enemyId = event.getEntity().getUniqueId();
        if (waveManager.getActiveEnemy(enemyId) == null) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        GamePlayer killerPlayer = killer != null ? game.GetPlayer(killer) : null;
        waveManager.handleEnemyDeath(enemyId, killerPlayer);
    }

    private GamePlayer resolveGamePlayer(Entity damager) {
        if (damager instanceof Player player) {
            return game.GetPlayer(player);
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return game.GetPlayer(player);
            }
        }
        return null;
    }
}
