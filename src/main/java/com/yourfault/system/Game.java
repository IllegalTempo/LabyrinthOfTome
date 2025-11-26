package com.yourfault.system;

import java.time.Duration;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

import com.yourfault.map.MapManager;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveDifficulty;
import com.yourfault.wave.WaveManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;

import com.yourfault.Main;
import com.yourfault.listener.PerkSelectionListener;
import com.yourfault.weapon.General.Projectile;

public class Game {
    private static final Title.Times GAME_START_TITLE_TIMES = Title.Times.times(
            Duration.ofMillis(500),
            Duration.ofSeconds(2),
            Duration.ofMillis(1000)
    );
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
            player.setInvulnerable(true);
            var gamePlayer = new GamePlayer(player);
            PLAYER_LIST.put(player.getUniqueId(), gamePlayer);
            showGameStartTitle(player);
            if (perkSelectionListener != null) {
                perkSelectionListener.preparePlayer(gamePlayer);
            }

        });
    }
    public void AddPlayer(Player player)
    {
        var gamePlayer = new GamePlayer(player);
        PLAYER_LIST.put(player.getUniqueId(), gamePlayer);
        showGameStartTitle(player);
        if (perkSelectionListener != null) {
            perkSelectionListener.preparePlayer(gamePlayer);
        }
        PlayerNumUpdate();
    }
    public void PlayerNumUpdate()
    {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Objects.requireNonNull(board.getTeam("00_PLAYERLIST_TOP")).suffix(Component.text(" [" + GetPlayerCount() + "]"));

    }
    public void RemovePlayer(Player player)
    {
        PLAYER_LIST.remove(player.getUniqueId());
        PlayerNumUpdate();

    }
    private void showGameStartTitle(Player player) {
        Title title = Title.title(
                Component.text("Game Started", NamedTextColor.GREEN),
                Component.text("Select your default weapon!", NamedTextColor.GRAY),
                GAME_START_TITLE_TIMES
        );
        player.showTitle(title);
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

        PLAYER_LIST.values().forEach((p) -> {
            p.resetProgress();
        });
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
    }
    public boolean WholeFamilyDies()
    {
        for (var player : PLAYER_LIST.values()) {
            if(player.CurrentState == GamePlayer.SurvivalState.ALIVE) {
                return false;
            }
        }
        return true;
    }
    public void CheckWholeFamilyDies()
    {
        if (WholeFamilyDies())
        {
            EndGame();
        }
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
