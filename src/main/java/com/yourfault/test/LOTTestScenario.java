package com.yourfault.test;

import com.yourfault.map.BossStructureSpawner;
import com.yourfault.map.MapManager;
import com.yourfault.map.MapTheme;
import com.yourfault.system.Game;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveDifficulty;
import com.yourfault.wave.WaveManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class LOTTestScenario {
    private static final int WAVES_PER_CYCLE = 9;
    private static final long BOSS_CLEAR_DELAY_TICKS = 20L * 30L;
    private static final long BOSS_TRANSITION_GAP_TICKS = 10L;
    private static final double LOCK_HEIGHT_OFFSET = 60.0;
    private static final int LOCK_EFFECT_DURATION_TICKS = 20 * 60;

    private final JavaPlugin plugin;
    private final Game game;
    private final MapManager mapManager;
    private final BossStructureSpawner bossSpawner;
    private final Random random = new Random();
    private final Map<UUID, PlayerLockState> lockedPlayers = new HashMap<>();

    private State state = State.IDLE;
    private BukkitTask monitorTask;
    private BukkitTask bossCleanupTask;
    private BukkitTask transitionGapTask;
    private CommandSender feedbackTarget;
    private Location scenarioCenter;
    private boolean loopActive;
    private boolean firstCycle = true;
    private MapTheme initialTheme;
    private int waveBaseline = 0;
    private int cycleTargetWave = WAVES_PER_CYCLE;

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
        feedbackTarget = feedback;
        scenarioCenter = center.clone();
        initialTheme = theme;
        ensurePlayersAdded(initiator);
        if (game.GetPlayerCount() == 0) {
            failAndReset("No players joined the game; LOT scenario aborted.");
            return;
        }
        loopActive = true;
        firstCycle = true;
        waveBaseline = 0;
        cycleTargetWave = WAVES_PER_CYCLE;
        feedback.sendMessage(ChatColor.YELLOW + "Starting LOT loop: generating arena...");
        startNextCycle();
    }

    public synchronized void forceStop(String reason) {
        if (state == State.IDLE) {
            return;
        }
        if (reason != null && feedbackTarget != null) {
            feedbackTarget.sendMessage(ChatColor.RED + reason);
        }
        loopActive = false;
        resetInternal();
        releaseLockedPlayers(resolveGroundLocation(scenarioCenter));
    }

    private synchronized void startNextCycle() {
        if (!loopActive) {
            return;
        }
        MapTheme theme = firstCycle && initialTheme != null
                ? initialTheme
                : MapTheme.pickRandom(random);
        firstCycle = false;
        state = State.GENERATING_MAP;
        cycleTargetWave = waveBaseline + WAVES_PER_CYCLE;
        feedbackTarget.sendMessage(ChatColor.YELLOW + "Generating map using theme " + theme.name().toLowerCase() + "...");
        mapManager.generateMapAsync(
                scenarioCenter,
                theme,
                summary -> {
                    synchronized (LOTTestScenario.this) {
                        if (!loopActive) {
                            return;
                        }
                        feedbackTarget.sendMessage(ChatColor.GREEN + "Map ready: " + summary.getTheme().name().toLowerCase() + " radius " + summary.getRadius() + ".");
                        releaseLockedPlayers(resolveSpawnDropLocation());
                        beginWaveRun();
                    }
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
        int displayStart = waveBaseline + 1;
        feedbackTarget.sendMessage(ChatColor.YELLOW + "Starting automated waves " + displayStart + "-" + cycleTargetWave + "...");
        game.StartGame(WaveDifficulty.EASY);
        waveManager.overrideCurrentWaveCounter(waveBaseline);
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
                    if (waveManager == null || !waveManager.isActive()) {
                        return;
                    }
                    if (waveManager.getCurrentWave() >= cycleTargetWave && !waveManager.isWaveInProgress()) {
                        waveManager.stop();
                        feedbackTarget.sendMessage(ChatColor.GREEN + "Wave " + cycleTargetWave + " cleared. Transitioning to boss room...");
                        state = State.TRANSITIONING_TO_BOSS;
                        cancel();
                        monitorTask = null;
                        beginBossTransition();
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private synchronized void beginBossTransition() {
        lockPlayersAboveArena();
        feedbackTarget.sendMessage(ChatColor.YELLOW + "Clearing arena before boss room...");
        mapManager.clearMapAsync(
                removed -> {
                    feedbackTarget.sendMessage(ChatColor.GREEN + "Arena cleared (" + removed + " blocks).");
                    scheduleBossGeneration();
                },
                error -> failAndReset("Failed to clear map: " + error)
        );
    }

    private synchronized void scheduleBossGeneration() {
        if (transitionGapTask != null) {
            transitionGapTask.cancel();
        }
        transitionGapTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            synchronized (LOTTestScenario.this) {
                if (!loopActive) {
                    return;
                }
                generateBossRoom();
            }
        }, BOSS_TRANSITION_GAP_TICKS);
    }

    private synchronized void generateBossRoom() {
        if (scenarioCenter == null) {
            failAndReset("Boss room center missing.");
            return;
        }
        state = State.GENERATING_BOSS;
        feedbackTarget.sendMessage(ChatColor.YELLOW + "Generating boss room...");
        bossSpawner.generateBossRoom(
                scenarioCenter,
                message -> {
                    synchronized (LOTTestScenario.this) {
                        if (!loopActive) {
                            return;
                        }
                        feedbackTarget.sendMessage(ChatColor.GREEN + message);
                        releaseLockedPlayers(resolveBossDropLocation());
                        scheduleBossCleanup();
                    }
                },
                error -> failAndReset("Failed to generate boss room: " + error)
        );
    }

    private synchronized void scheduleBossCleanup() {
        state = State.BOSS_ROOM_ACTIVE;
        feedbackTarget.sendMessage(ChatColor.YELLOW + "Boss room active for 30 seconds.");
        if (bossCleanupTask != null) {
            bossCleanupTask.cancel();
        }
        bossCleanupTask = plugin.getServer().getScheduler().runTaskLater(plugin, this::handleBossTimeout, BOSS_CLEAR_DELAY_TICKS);
    }

    private void handleBossTimeout() {
        synchronized (this) {
            if (!loopActive) {
                return;
            }
            feedbackTarget.sendMessage(ChatColor.YELLOW + "Boss timer elapsed. Resetting arena...");
            lockPlayersAboveArena();
            state = State.CLEARING_BOSS;
        }
        bossSpawner.clearBossRoom(
                message -> {
                    synchronized (LOTTestScenario.this) {
                        if (!loopActive) {
                            return;
                        }
                        feedbackTarget.sendMessage(ChatColor.GREEN + message);
                        advanceWaveBaseline();
                        startNextCycle();
                    }
                },
                error -> failAndReset("Failed to clear boss room: " + error)
        );
    }

    private synchronized void failAndReset(String reason) {
        loopActive = false;
        if (feedbackTarget != null) {
            feedbackTarget.sendMessage(ChatColor.RED + reason);
        }
        resetInternal();
        releaseLockedPlayers(resolveGroundLocation(scenarioCenter));
    }

    private synchronized void resetInternal() {
        state = State.IDLE;
        if (monitorTask != null) {
            monitorTask.cancel();
            monitorTask = null;
        }
        if (bossCleanupTask != null) {
            bossCleanupTask.cancel();
            bossCleanupTask = null;
        }
        if (transitionGapTask != null) {
            transitionGapTask.cancel();
            transitionGapTask = null;
        }
        if (!loopActive) {
            feedbackTarget = null;
            scenarioCenter = null;
            initialTheme = null;
            firstCycle = true;
            waveBaseline = 0;
            cycleTargetWave = WAVES_PER_CYCLE;
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

    private void lockPlayersAboveArena() {
        if (scenarioCenter == null) {
            return;
        }
        World world = scenarioCenter.getWorld();
        if (world == null) {
            return;
        }
        Location hover = scenarioCenter.clone();
        hover.add(0.5, 0, 0.5);
        hover.setY(Math.max(world.getMinHeight() + 20, scenarioCenter.getY() + LOCK_HEIGHT_OFFSET));
        for (GamePlayer gp : game.PLAYER_LIST.values()) {
            Player player = gp.getMinecraftPlayer();
            if (player == null) {
                continue;
            }
            lockedPlayers.computeIfAbsent(player.getUniqueId(), id -> PlayerLockState.from(player));
            freezePlayer(player, hover);
        }
    }

    private void freezePlayer(Player player, Location target) {
        player.setVelocity(new Vector(0, 0, 0));
        player.teleport(target);
        player.setGravity(false);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, LOCK_EFFECT_DURATION_TICKS, 10, false, false, false));
    }

    private void releaseLockedPlayers(Location destination) {
        if (lockedPlayers.isEmpty()) {
            return;
        }
        Location baseDrop = destination == null ? null : destination.clone();
        for (Map.Entry<UUID, PlayerLockState> entry : lockedPlayers.entrySet()) {
            PlayerLockState snapshot = entry.getValue();
            Player player = snapshot.resolvePlayer();
            if (player == null) {
                continue;
            }
            snapshot.restore(player);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            if (baseDrop != null) {
                player.teleport(baseDrop.clone());
            }
        }
        lockedPlayers.clear();
    }

    private Location resolveSpawnDropLocation() {
        List<Location> markers = mapManager.getSpawnMarkers();
        if (!markers.isEmpty()) {
            Location marker = markers.get(0).clone();
            marker.add(0.5, 1.0, 0.5);
            return marker;
        }
        return resolveGroundLocation(scenarioCenter);
    }

    private Location resolveBossDropLocation() {
        return resolveGroundLocation(scenarioCenter);
    }

    private Location resolveGroundLocation(Location base) {
        if (base == null || base.getWorld() == null) {
            return null;
        }
        Location copy = base.clone();
        World world = base.getWorld();
        int top = world.getHighestBlockYAt(copy);
        copy.setY(top + 1.0);
        copy.add(0.5, 0, 0.5);
        return copy;
    }

    private void advanceWaveBaseline() {
        waveBaseline += WAVES_PER_CYCLE + 1;
        cycleTargetWave = waveBaseline + WAVES_PER_CYCLE;
    }

    private enum State {
        IDLE,
        GENERATING_MAP,
        RUNNING_WAVES,
        TRANSITIONING_TO_BOSS,
        GENERATING_BOSS,
        BOSS_ROOM_ACTIVE,
        CLEARING_BOSS
    }

    private static final class PlayerLockState {
        private final UUID playerId;
        private final boolean gravity;
        private final boolean allowFlight;
        private final boolean flying;

        private PlayerLockState(UUID playerId, boolean gravity, boolean allowFlight, boolean flying) {
            this.playerId = playerId;
            this.gravity = gravity;
            this.allowFlight = allowFlight;
            this.flying = flying;
        }

        private static PlayerLockState from(Player player) {
            return new PlayerLockState(player.getUniqueId(), player.hasGravity(), player.getAllowFlight(), player.isFlying());
        }

        private Player resolvePlayer() {
            return playerId == null ? null : Bukkit.getPlayer(playerId);
        }

        private void restore(Player player) {
            if (player == null) {
                return;
            }
            player.setGravity(gravity);
            player.setAllowFlight(allowFlight);
            player.setFlying(allowFlight && flying);
        }
    }
}
