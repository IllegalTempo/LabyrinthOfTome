package com.yourfault.system;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import com.yourfault.Enemy.Enemy;
import static com.yourfault.Main.plugin;
import com.yourfault.listener.ChainLinkManager;
import com.yourfault.system.GeneralPlayer.GamePlayer;

/**
 * Redirects vanilla damage into the custom GamePlayer health pool.
 */
public class CustomHealthListener implements Listener {
    private final Game game;

    public CustomHealthListener(Game game) {
        this.game = game;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {


        if (event.getEntity() instanceof LivingEntity living) {
            Player damager = null;
            if (event.getDamageSource() != null && event.getDamageSource().getCausingEntity() instanceof Player p) {
                damager = p;
            }
            if (ChainLinkManager.handleLeadDamage(living, (float) event.getFinalDamage(), damager)) {
                event.setCancelled(true);
                return;
            }
        }

        double damage = event.getFinalDamage();
        if (damage <= 0) {
            return;
        }
        Entity causingEntity = null;
        if (event.getDamageSource() != null) {
            causingEntity = event.getDamageSource().getCausingEntity();
        }

        if (game.ENEMY_LIST.containsKey(event.getEntity().getUniqueId())) {
            Enemy enemy = game.ENEMY_LIST.get(event.getEntity().getUniqueId());
            if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                LabyrinthCreature damageDealer = (causingEntity != null) ? game.CREATURE_LIST.get(causingEntity.getUniqueId()) : null;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    enemy.applyDamage((float) damage, damageDealer, false);

                }, 5L);

            } else {
                enemy.applyDamage((float) damage, null, false);

            }
            event.setCancelled(true);
        }
        if ((event.getEntity() instanceof Player player)) {
            GamePlayer gamePlayer = game.GetPlayer(player);
            if (gamePlayer != null) {
                event.setDamage(0);
                LabyrinthCreature damageDealer = (causingEntity != null) ? game.CREATURE_LIST.getOrDefault(causingEntity.getUniqueId(), null) : null;
                gamePlayer.applyDamage((float) damage, damageDealer, false);
            }
        }


    }

}
