package com.yourfault.system;

import com.yourfault.Main;
import com.yourfault.listener.WeaponSelectionListener;
import com.yourfault.weapon.General.Projectile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class Game {
    public Game(JavaPlugin plugin)
    {
        this.plugin = plugin;
    }
    private JavaPlugin plugin;
    private BukkitRunnable StatDisplayTask;
    public Vector Gravity = new Vector(0,-0.5,0);
    private HashMap<UUID,Player> PLAYER_LIST = new HashMap<>();
    public HashMap<UUID,Projectile> PROJECTILE_LIST = new HashMap<>();
    public HashMap<UUID,Enemy> ENEMY_LIST = new HashMap<>();
    public void StartGame()
    {
        Main.world.getPlayers().forEach(player -> {
            PLAYER_LIST.put(player.getUniqueId(),new Player(player,100f,100f,100f, WeaponSelectionListener.WeaponType.Excalibur));
            player.sendTitle("§aGame Started","§7Wow!",10,70,20);
        });
        DisplayPlayerStat();
    }
    public void EndGame()
    {
        PLAYER_LIST.clear();
        if(StatDisplayTask != null)StatDisplayTask.cancel();
    }
    public Player GetPlayer(UUID uuid)
    {
        return PLAYER_LIST.get(uuid);
    }
    public void DisplayPlayerStat() {
        StatDisplayTask = new BukkitRunnable() {
            @Override
            public void run() {
                PLAYER_LIST.values().forEach(Player::DisplayStatToPlayer);
            }
        };
        StatDisplayTask.runTaskTimer(Main.plugin, 0L, 1L);
    }

}
