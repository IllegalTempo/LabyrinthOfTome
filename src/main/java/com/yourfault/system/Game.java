package com.yourfault.system;

import java.util.HashMap;
import java.util.UUID;

import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.yourfault.Main;
import com.yourfault.listener.PerkSelectionListener;
import com.yourfault.weapon.General.Projectile;

public class Game {
    public Game(JavaPlugin plugin)
    {
        this.plugin = plugin;
    }
    private final JavaPlugin plugin;
    private BukkitRunnable StatDisplayTask;
    public Vector Gravity = new Vector(0,-0.5,0);
    public HashMap<UUID, GamePlayer> PLAYER_LIST = new HashMap<>();
    public HashMap<UUID,Projectile> PROJECTILE_LIST = new HashMap<>();
    public HashMap<UUID,Enemy> ENEMY_LIST = new HashMap<>();
    private PerkSelectionListener perkSelectionListener;
    public boolean isGameRunning()
    {
        return !PLAYER_LIST.isEmpty();
    }
    public void StartGame()
    {
        CleanPlayerList();
        Main.world.getPlayers().forEach(player -> {
            var gamePlayer = new GamePlayer(player);
            PLAYER_LIST.put(player.getUniqueId(), gamePlayer);
            player.sendTitle("§aGame Started","§7Select your default weapon!",10,40,20);
            if (perkSelectionListener != null) {
                perkSelectionListener.preparePlayer(gamePlayer);
            }

        });
        DisplayPlayerStat();
    }
    public void EndGame()
    {
        CleanPlayerList();
        if(StatDisplayTask != null)StatDisplayTask.cancel();
    }
    private void CleanPlayerList()
    {
        PLAYER_LIST.values().forEach(p -> {
            var bukkitPlayer = p.getMinecraftPlayer();
            if (bukkitPlayer != null) {
                bukkitPlayer.getInventory().setItem(0, null);
                p.PLAYER_PERKS.removePerks();
            }
            p.PLAYER_PERKS.clearPerks();
            p.SELECTED_WEAPON = null;
        });
        PLAYER_LIST.clear();
    }
    public GamePlayer GetPlayer(UUID uuid)
    {
        return PLAYER_LIST.get(uuid);
    }
    public GamePlayer GetPlayer(Player player){return GetPlayer(player.getUniqueId());}
    public void DisplayPlayerStat() {
        StatDisplayTask = new BukkitRunnable() {
            @Override
            public void run() {
                PLAYER_LIST.values().forEach(GamePlayer::DisplayStatToPlayer);
            }
        };
        StatDisplayTask.runTaskTimer(Main.plugin, 0L, 1L);
    }



    public void setPerkSelectionListener(PerkSelectionListener perkSelectionListener) {
        this.perkSelectionListener = perkSelectionListener;
    }


}
