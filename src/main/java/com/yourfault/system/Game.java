package com.yourfault.system;

import java.util.HashMap;
import java.util.UUID;

import com.yourfault.map.MapManager;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveDifficulty;
import com.yourfault.wave.WaveManager;
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
        AddExistingPlayer();
        Main_Update();

    }
    private final JavaPlugin plugin;
    private BukkitRunnable UpdateTask;
    public Vector Gravity = new Vector(0,-0.5,0);
    public HashMap<UUID, GamePlayer> PLAYER_LIST = new HashMap<>();
    public HashMap<UUID,Projectile> PROJECTILE_LIST = new HashMap<>();
    public HashMap<UUID,Enemy> ENEMY_LIST = new HashMap<>();
    private PerkSelectionListener perkSelectionListener;
    public boolean isGameRunning()
    {
        return !PLAYER_LIST.isEmpty();
    }
    private WaveManager waveManager;
    private MapManager mapManager;
    private void AddExistingPlayer()
    {
        Main.world.getPlayers().forEach(player -> {
            var gamePlayer = new GamePlayer(player);
            PLAYER_LIST.put(player.getUniqueId(), gamePlayer);
            player.sendTitle("§aGame Started","§7Select your default weapon!",10,40,20);
            if (perkSelectionListener != null) {
                perkSelectionListener.preparePlayer(gamePlayer);
            }

        });
    }
    public void AddPlayer(Player player)
    {
        var gamePlayer = new GamePlayer(player);
        PLAYER_LIST.put(player.getUniqueId(), gamePlayer);
        player.sendTitle("§aGame Started","§7Select your default weapon!",10,40,20);
        if (perkSelectionListener != null) {
            perkSelectionListener.preparePlayer(gamePlayer);
        }
    }
    public void RemovePlayer(Player player)
    {
        PLAYER_LIST.remove(player.getUniqueId());
        
    }
    public void StartGame(WaveDifficulty difficulty)
    {
        //CleanPlayerList();
        PLAYER_LIST.values().forEach(GamePlayer::resetProgress);
        preparePerkItemsForAllPlayers();
        if (waveManager != null) {
            waveManager.initializeSession(difficulty);
        }

    }
    private void Main_Update()
    {
        UpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                PLAYER_LIST.values().forEach(GamePlayer::DisplayStatToPlayer);

            }
        };
        UpdateTask.runTaskTimer(plugin, 0L, 1L);
    }

    public void EndGame()
    {
        //CleanPlayerList();
        PLAYER_LIST.values().forEach(player -> player.PLAYER_PERKS.removePerks());
        if (waveManager != null) {
            int cleared = waveManager.clearAllEnemiesInstantly(true);
            if (cleared > 0) {
                plugin.getLogger().info(String.format("Cleared %d active wave enemies during EndGame.", cleared));
            }
            waveManager.stop();
        }
        if (mapManager != null && mapManager.hasActiveMap()) {
            mapManager.clearMapAsync(
                    removed -> plugin.getLogger().info(String.format("Cleared map after EndGame (%d blocks).", removed)),
                    error -> plugin.getLogger().warning(String.format("Failed to clear map after EndGame: %s", error))
            );
        }
        if(UpdateTask != null)UpdateTask.cancel();
    }
//    private void CleanPlayerList()
//    {
//        PLAYER_LIST.values().forEach(p -> {
//            var bukkitPlayer = p.getMinecraftPlayer();
//            if (bukkitPlayer != null) {
//                bukkitPlayer.getInventory().setItem(0, null);
//                p.PLAYER_PERKS.removePerks();
//            }
//            p.PLAYER_PERKS.clearPerks();
//            p.SELECTED_WEAPON = null;
//        });
//        PLAYER_LIST.clear();
//    }
    public GamePlayer GetPlayer(UUID uuid)
    {
        return PLAYER_LIST.get(uuid);
    }
    public GamePlayer GetPlayer(Player player){return GetPlayer(player.getUniqueId());}


    public void setPerkSelectionListener(PerkSelectionListener perkSelectionListener) {
        this.perkSelectionListener = perkSelectionListener;
        preparePerkItemsForAllPlayers();
    }

    public void setWaveManager(WaveManager waveManager) {
        this.waveManager = waveManager;
    }

    public WaveManager getWaveManager() {
        return waveManager;
    }

    public void setMapManager(MapManager mapManager) {
        this.mapManager = mapManager;
    }

    public MapManager getMapManager() {
        return mapManager;
    }
    public int GetPlayerCount()
    {
        return PLAYER_LIST.size();
    }

    private void preparePerkItemsForAllPlayers() {
        if (perkSelectionListener == null) {
            return;
        }
        PLAYER_LIST.values().forEach(perkSelectionListener::preparePlayer);
    }
}
