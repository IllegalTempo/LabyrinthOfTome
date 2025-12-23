package com.yourfault.system;

import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.util.Objects;
import java.util.UUID;

/**
 * Handles the bleeding (downed) state for players, including timers and revive interactions.
 */
public class BleedoutManager implements Listener {
    public static final int BLEED_OUT_SECONDS = 60;
    public static final int REVIVE_SECONDS = 5;
    public static final double REVIVE_RADIUS = 3.5;

    private static final PotionEffectType EFFECT_SLOW = resolveEffect("SLOW", "SLOWNESS");
    private static final PotionEffectType EFFECT_WEAKNESS = resolveEffect("WEAKNESS");
    private static final PotionEffectType EFFECT_BLINDNESS = resolveEffect("BLINDNESS");

    private final JavaPlugin plugin;
    private final Game game;
//    private final Map<UUID, BleedoutState> downedPlayers = new HashMap<>();
//    private final Map<UUID, ReviveProcess> activeRevives = new HashMap<>();

    public BleedoutManager(JavaPlugin plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
    }

    @EventHandler
    public void onSneakToggle(PlayerToggleSneakEvent event) {
        Player rescuer = event.getPlayer();
        GamePlayer rescuerGamePlayer = Main.game.GetPlayer(rescuer);
        if (rescuerGamePlayer.CurrentState != GamePlayer.SurvivalState.ALIVE) {
            rescuer.sendMessage(ChatColor.RED + "You cannot revive others while bleeding out.");
            return;
        }
        if(event.isSneaking())
        {
            GamePlayer Target = findNearbyDowned(rescuer);
            if (Target == null) {
                Bukkit.getConsoleSender().sendMessage("[LOT ERROR] NULL REVIVE TARGET");
                return;
            }
            rescuerGamePlayer.beginRevive(Target);
        } else {
            rescuerGamePlayer.CancelRevive();
        }



    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        Main.game.GetPlayer(event.getPlayer()).CancelRevive();
    }

    private GamePlayer findNearbyDowned(Player rescuer) {
        for (Entity entity : rescuer.getNearbyEntities(REVIVE_RADIUS, REVIVE_RADIUS, REVIVE_RADIUS)) {
            if (entity instanceof Mannequin) {
                //log getDeadPlayer
                Bukkit.getConsoleSender().sendMessage("[LOT DEBUG] deadplayerl" + Main.game.getDeadPlayer.size());

                if (Main.game.getDeadPlayer.containsKey(entity.getUniqueId()))
                {
                    return Main.game.getDeadPlayer.get(entity.getUniqueId());
                }
            }

        }
        return null;
    }
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = Main.game.GetPlayer(player);
        if(gamePlayer.CurrentState == GamePlayer.SurvivalState.DOWNED)
        {
            event.setCancelled(true);
        }
    }

    private static PotionEffectType resolveEffect(String... names) {
        for (String name : names) {
            PotionEffectType type = PotionEffectType.getByName(name);
            if (type != null) {
                return type;
            }
        }
        return null;
    }
}
