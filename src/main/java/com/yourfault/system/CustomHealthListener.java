package com.yourfault.system;

import com.yourfault.Enemy.Enemy;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import static com.yourfault.Main.plugin;

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


        double damage = event.getFinalDamage();
        if (damage <= 0) {
            return;
        }
        if(game.ENEMY_LIST.containsKey(event.getEntity().getUniqueId()))
        {
            Enemy enemy = game.ENEMY_LIST.get(event.getEntity().getUniqueId());
            if(event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK)
            {
                GamePlayer damageDealer = event.getDamageSource().getCausingEntity() instanceof Player p ? game.GetPlayer(p) : null;
                Bukkit.getScheduler().runTaskLater(plugin,()-> {
                    enemy.OnBeingDamage((float) damage,damageDealer);

                },5L);

            }else {
                enemy.OnBeingDamage((float) damage,null);

            }
            event.setCancelled(true);
        }
        if ((event.getEntity() instanceof Player player)) {
            GamePlayer gamePlayer = game.GetPlayer(player);
            if (gamePlayer != null) {
                event.setDamage(0);
                gamePlayer.damage((float) damage);
            }
        }


    }

}
