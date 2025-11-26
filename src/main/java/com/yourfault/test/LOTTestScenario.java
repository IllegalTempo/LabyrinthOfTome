package com.yourfault.test;

import com.yourfault.Main;
import com.yourfault.map.BossStructureSpawner;
import com.yourfault.map.MapManager;
import com.yourfault.map.MapTheme;
import com.yourfault.system.Game;
import com.yourfault.wave.WaveDifficulty;
import com.yourfault.wave.WaveManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Objects;

public class LOTTestScenario {
    private static final int TARGET_WAVE = 9;
    private static final long BOSS_CLEAR_DELAY_TICKS = 20L * 30L;

    private final JavaPlugin plugin;
    private final Game game;
    private final MapManager mapManager;
    private final BossStructureSpawner bossSpawner;

    private State state = State.IDLE;
    private BukkitTask monitorTask;
    private BukkitTask bossCleanupTask;
    private CommandSender feedbackTarget;
    private Location scenarioCenter;

    public LOTTestScenario(JavaPlugin plugin,
                           Game game,
                           MapManager mapManager,
                           BossStructureSpawner bossSpawner) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.game = Objects.requireNonNull(game, "game");
        this.mapManager = Objects.requireNonNull(mapManager, "mapManager");
        this.bossSpawner = Objects.requireNonNull(bossSpawner, "bossSpawner");
    }

    public synchronized void startScenario(Player initiator,
                                           Location center,
                                           MapTheme theme,
                                           CommandSender feedback) {
        Objects.requireNonNull(center, "center");
        Objects.requireNonNull(feedback, "feedback");
        Objects.requireNonNull(initiator, "initiator");
        if (state != State.IDLE) {
            feedback.sendMessage(ChatColor.RED + "A LOT stress test is already running.");
            return;
        }
        state = State.GENERATING_MAP;
        feedbackTarget = feedback;
        scenarioCenter = center.clone();
        ensurePlayersAdded(initiator);
        if (game.GetPlayerCount() == 0) {
            failAndReset("No players joined the game; LOT scenario aborted.");
            return;
        }
        feedback.sendMessage(ChatColor.YELLOW + "Starting LOT test scenario: generating map...");
        mapManager.generateMapAsync(
                center,
                theme,
                summary -> {
                    feedback.sendMessage(ChatColor.GREEN + "Map generated: " + summary.getTheme().name().toLowerCase() + " (radius " + summary.getRadius() + ").");
                    beginWaveRun();
                },
                error -> failAndReset("Map generation failed: " + error)
        );
    }

    private synchronized void beginWaveRun() {
        WaveManager waveManager = game.getWaveManager();
        if (waveManager == null) {
            failAndReset("Wave manager unavailable.");
            return;
        }
        state = State.RUNNING_WAVES;
        feedbackTarget.sendMessage(ChatColor.YELLOW + "Starting automated wave run (1-" + TARGET_WAVE + ")...");
        game.StartGame(WaveDifficulty.EASY);
        waveManager.triggerNextWave();
        startWaveMonitor();
    }

    private void startWaveMonitor() {
        if (monitorTask != null) {
            monitorTask.cancel();
        }
        monitorTask = new BukkitRunnable() {
            @Override
            public void run() {
                synchronized (LOTTestScenario.this) {
                    if (state != State.RUNNING_WAVES) {
                        return;
                    }
                    WaveManager waveManager = game.getWaveManager();
                    if (waveManager == null) {
                        failAndReset("Wave manager unavailable.");
                        cancel();
                        return;
                    }
                    if (!waveManager.isActive()) {
                        return;
                    }
                    if (waveManager.getCurrentWave() >= TARGET_WAVE && !waveManager.isWaveInProgress()) {
                        waveManager.stop();
                        feedbackTarget.sendMessage(ChatColor.GREEN + "Wave " + TARGET_WAVE + " cleared. Proceeding to teardown...");
                        state = State.CLEARING_MAP;
                        cancel();
                        monitorTask = null;
                        startMapClear();
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private synchronized void startMapClear() {
        feedbackTarget.sendMessage(ChatColor.YELLOW + "Clearing generated map before boss phase...");
        mapManager.clearMapAsync(
                removed -> {
                    feedbackTarget.sendMessage(ChatColor.GREEN + "Cleared map and removed " + removed + " blocks.");
                    startBossPhase();
                },
                error -> failAndReset("Failed to clear map: " + error)
        );
    }

    private synchronized void startBossPhase() {
        if (scenarioCenter == null) {
            failAndReset("Boss room center missing.");
            return;
        }
        state = State.GENERATING_BOSS;
        feedbackTarget.sendMessage(ChatColor.YELLOW + "Generating boss room...");
        bossSpawner.generateBossRoom(
                scenarioCenter,
                message -> {
                    feedbackTarget.sendMessage(ChatColor.GREEN + message);
                    scheduleBossCleanup();
                },
                error -> failAndReset("Failed to generate boss room: " + error)
        );
    }

    private synchronized void scheduleBossCleanup() {
        state = State.BOSS_ROOM_ACTIVE;
        feedbackTarget.sendMessage(ChatColor.YELLOW + "Boss room will auto-clear in 30 seconds...");
        if (bossCleanupTask != null) {
            bossCleanupTask.cancel();
        }
        bossCleanupTask = plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        bossSpawner.clearBossRoom(
                                message -> {
                                    feedbackTarget.sendMessage(ChatColor.GREEN + message);
                                    feedbackTarget.sendMessage(ChatColor.GREEN + "LOT test scenario complete.");
                                    reset();
                                },
                                error -> failAndReset("Failed to clear boss room: " + error)
                        ),
                BOSS_CLEAR_DELAY_TICKS
        );
    }

    private synchronized void failAndReset(String reason) {
        if (feedbackTarget != null) {
            feedbackTarget.sendMessage(ChatColor.RED + reason);
        }
        reset();
    }

    private synchronized void reset() {
        state = State.IDLE;
        scenarioCenter = null;
        feedbackTarget = null;
        if (monitorTask != null) {
            monitorTask.cancel();
            monitorTask = null;
        }
        if (bossCleanupTask != null) {
            bossCleanupTask.cancel();
            bossCleanupTask = null;
        }
    }

    public synchronized boolean isIdle() {
        return state == State.IDLE;
    }

    private void ensurePlayersAdded(Player initiator) {
        if (game.GetPlayerCount() > 0) {
            return;
        }
        List<Player> players = initiator.getWorld().getPlayers();
        for (Player online : players) {
            if (game.GetPlayer(online) == null) {
                game.AddPlayer(online);
            }
        }
    }

    private enum State {
        IDLE,
        GENERATING_MAP,
        RUNNING_WAVES,
        CLEARING_MAP,
        GENERATING_BOSS,
        BOSS_ROOM_ACTIVE
    }
}
