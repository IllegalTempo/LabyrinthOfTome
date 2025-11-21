package com.yourfault.system;

import com.yourfault.Main;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Game {
    public Game(JavaPlugin plugin)
    {
        this.plugin = plugin;
    }
    private JavaPlugin plugin;
    private BukkitRunnable StatDisplayTask;
    private HashMap<UUID,Player> PLAYER_LIST = new HashMap<>();
    public void StartGame()
    {
        Bukkit.getWorld("world").getPlayers().forEach(player -> {
            PLAYER_LIST.put(player.getUniqueId(),new Player(player,100f,100f));
            player.sendTitle("§aGame Started","§7Wow!",10,70,20);
        });
        START_DisplayPlayerStat();
    }
    public void EndGame()
    {
        Main.game = null;
        PLAYER_LIST.clear();
        if(StatDisplayTask != null)StatDisplayTask.cancel();
    }
    public Player GetPlayer(UUID uuid)
    {
        return PLAYER_LIST.get(uuid);
    }
    public void START_DisplayPlayerStat() {
        StatDisplayTask = new BukkitRunnable() {
            @Override
            public void run() {
                PLAYER_LIST.values().forEach(Player::DisplayStatToPlayer);
            }
        };
        StatDisplayTask.runTaskTimer(Main.plugin, 0L, 1L);
    }
}
