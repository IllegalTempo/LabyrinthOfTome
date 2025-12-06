package com.yourfault.gameloop;

import com.yourfault.Main;
import com.yourfault.map.BossStructureSpawner;
import com.yourfault.map.MapGenerationSummary;
import com.yourfault.map.MapManager;
import com.yourfault.system.Game;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveEncounterType;
import com.yourfault.wave.WaveLifecycleListener;
import com.yourfault.wave.WaveManager;
import com.yourfault.wave.WaveDifficulty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class GameLoopManager implements WaveLifecycleListener {
    private static final int WAVES_PER_CYCLE = 10;
    private static final long NEXT_WAVE_DELAY_TICKS = 100L;
    private static final long BOSS_ROOM_READY_POLL_TICKS = 40L;
    private static final int FALL_IMMUNITY_TICKS = 20 * 12;
    private static final long INITIAL_READY_TIMEOUT_TICKS = 20L * 60;
    private static final long FINAL_WAVE_HOLD_TICKS = 20L * 3;
    private static final double SPAWN_BORDER_BUFFER = 8.0;
    private static final int MAX_SPAWN_ATTEMPTS = 24;
    private static final Title.Times STANDARD_TITLE_TIMES = Title.Times.times(
            Duration.ofMillis(250),
            Duration.ofSeconds(2),
            Duration.ofMillis(400)
    );

    private final JavaPlugin plugin;
    private final Game game;
    private final MapManager mapManager;
    private final BossStructureSpawner bossSpawner;
    private final WaveManager waveManager;
    private final GameLoopConfig config = new GameLoopConfig();
    private final Random random = new Random();

    private GameLoopPhase phase = GameLoopPhase.IDLE;
    private ReadyCheck activeReadyCheck;
    private ReadyStage readyStage = ReadyStage.INITIAL;
    private boolean loopActive = false;
    private boolean waveControlEnabled = false;
    private boolean sessionInitialized = false;
    private int wavesClearedInCycle = 0;
    private int cycleIndex = 0;
    private BukkitTask scheduledWaveTask;
    private BukkitTask bossCountdownTask;
    private BukkitTask readyTimeoutTask;
    private boolean bossPrepNoticeActive = false;
    private boolean playersConfirmedNextCycle = false;
    private boolean pendingArenaReady = false;
    private boolean waitingForMapNoticeSent = false;
    private boolean arenaReadyWaitingOnPlayersNoticeSent = false;
    private boolean awaitingBossRoomClear = false;
    private Location pendingArenaCenter;
    private Location pendingStartScatter;
    private final Set<UUID> bossSpectatorIds = new HashSet<>();

    private enum ReadyStage {
        INITIAL,
        POST_BOSS
    }

    public GameLoopManager(JavaPlugin plugin,
                           Game game,
                           MapManager mapManager,
                           BossStructureSpawner bossSpawner,
                           WaveManager waveManager) {
        this.plugin = plugin;
        this.game = game;
        this.mapManager = mapManager;
        this.bossSpawner = bossSpawner;
        this.waveManager = waveManager;
    }

    public GameLoopConfig getConfig() {
        return config;
    }

    public boolean startPlayCommand(Player initiator) {
        if (phase != GameLoopPhase.IDLE && phase != GameLoopPhase.ENDED) {
            initiator.sendMessage(ChatColor.YELLOW + "A game loop is already running.");
            return false;
        }
        List<Player> participants = getOnlineParticipants();
        if (participants.isEmpty()) {
            initiator.sendMessage(ChatColor.RED + "No active players to start a session.");
            return false;
        }
        hardResetState();
        loopActive = true;
        readyStage = ReadyStage.INITIAL;
        phase = GameLoopPhase.INITIAL_READY;
        activeReadyCheck = new ReadyCheck(toUuidList(participants), ReadyCheck.Mode.YES_NO);
        scheduleReadyTimeout();
        Bukkit.broadcastMessage(ChatColor.AQUA + "Play session requested by " + initiator.getName() + ". Waiting for everyone to accept.");
        participants.forEach(this::sendInitialReadyPrompt);
        return true;
    }

    public boolean handleReadyCommand(Player player, ReadyAction action) {
        if (activeReadyCheck == null) {
            player.sendMessage(ChatColor.YELLOW + "There is no pending ready check right now.");
            return false;
        }
        UUID id = player.getUniqueId();
        if (!activeReadyCheck.contains(id)) {
            player.sendMessage(ChatColor.YELLOW + "You are not part of the current ready check.");
            return false;
        }
        ReadyCheck.Mode mode = activeReadyCheck.mode();
        boolean stateChanged = false;
        if (mode == ReadyCheck.Mode.YES_NO) {
            if (action == ReadyAction.NO) {
                if (activeReadyCheck.markDeclined(id)) {
                    Bukkit.broadcastMessage(ChatColor.RED + player.getName() + " needs DOWNTIME!");
                    cancelReadyTimeout();
                    failAndShutdown(player.getName() + " cancelled the ready check.");
                }
                return true;
            }
            if (action != ReadyAction.YES) {
                player.sendMessage(ChatColor.YELLOW + "Use /ready yes or click the green button to ready up.");
                return false;
            }
            stateChanged = activeReadyCheck.markReady(id);
        } else {
            if (action == ReadyAction.NO) {
                player.sendMessage(ChatColor.YELLOW + "Everyone must confirm with /ready to continue.");
                return false;
            }
            stateChanged = activeReadyCheck.markReady(id);
        }
        if (stateChanged) {
            int ready = activeReadyCheck.readyCount();
            int total = activeReadyCheck.totalParticipants();
            Bukkit.broadcastMessage(ChatColor.GREEN + player.getName() + " has ready up (" + ready + "/" + total + ").");
        } else {
            player.sendMessage(ChatColor.GRAY + "Your response is already recorded.");
        }
        if (activeReadyCheck.isComplete()) {
            ReadyCheck completedCheck = activeReadyCheck;
            activeReadyCheck = null;
            cancelReadyTimeout();
            onReadyCheckComplete(completedCheck.mode());
        }
        return true;
    }

    public void onEncounterStarted(int waveNumber, WaveEncounterType type, int enemyCount) {
        if (!loopActive) {
            return;
        }
        if (type == WaveEncounterType.STANDARD) {
            sendWaveTitle(waveNumber, enemyCount);
        } else {
            sendBossTitle();
        }
    }

    public void onEncounterCompleted(int waveNumber, WaveEncounterType type) {
        if (!loopActive) {
            return;
        }
        if (type == WaveEncounterType.STANDARD) {
            sendWaveClearedTitle(waveNumber);
            wavesClearedInCycle++;
            if (wavesClearedInCycle >= WAVES_PER_CYCLE) {
                beginBossCountdown();
            } else {
                scheduleNextWave();
            }
        } else {
            handleBossDefeated();
        }
    }

    public void handleExternalGameEnd() {
        clearActiveMapOnShutdown();
        hardResetState();
        sessionInitialized = false;
        phase = GameLoopPhase.ENDED;
    }

    private void onReadyCheckComplete(ReadyCheck.Mode completedMode) {
        if (completedMode == ReadyCheck.Mode.YES_NO && readyStage == ReadyStage.INITIAL) {
            startInitialLoop();
            readyStage = ReadyStage.POST_BOSS;
            return;
        }
        if (completedMode == ReadyCheck.Mode.CONFIRM_ONLY && readyStage == ReadyStage.POST_BOSS) {
            cycleIndex++;
            playersConfirmedNextCycle = true;
            waitingForMapNoticeSent = false;
            attemptLaunchNextCycle();
        }
    }

    private void startInitialLoop() {
        enableWaveControl();
        if (!sessionInitialized) {
            game.StartGame(WaveDifficulty.MEDIUM);
            sessionInitialized = true;
        }
        prepareNextCycleLaunchState(true);
        generateCombatMap();
    }

    private void prepareNextCycleLaunchState(boolean playersConfirmed) {
        playersConfirmedNextCycle = playersConfirmed;
        pendingArenaReady = false;
        waitingForMapNoticeSent = false;
        arenaReadyWaitingOnPlayersNoticeSent = false;
        pendingArenaCenter = null;
        pendingStartScatter = null;
        bossPrepNoticeActive = false;
    }

    private void generateCombatMap() {
        World world = resolveWorld();
        if (world == null) {
            failAndShutdown("No world loaded for map generation.");
            return;
        }
        phase = GameLoopPhase.MAP_GENERATING;
        pendingArenaReady = false;
        waitingForMapNoticeSent = false;
        arenaReadyWaitingOnPlayersNoticeSent = false;
        pendingArenaCenter = null;
        pendingStartScatter = null;
        bossPrepNoticeActive = false;
        Location center = config.resolvePlayMapCenter(world);
        Bukkit.broadcastMessage(ChatColor.YELLOW + "Map is currently generating...");
        mapManager.generateMapAsync(center,
                summary -> Bukkit.getScheduler().runTask(plugin, () -> onMapGenerated(center, summary)),
                error -> Bukkit.getScheduler().runTask(plugin, () -> failAndShutdown(error)));
        prepareBossArena(world);
    }

    private void onMapGenerated(Location center, MapGenerationSummary summary) {
        if (!loopActive) {
            return;
        }
        String themeName = summary.getTheme() != null ? summary.getTheme().name() : "Unknown";
        Bukkit.broadcastMessage(ChatColor.GREEN + "Arena ready with theme " + themeName + ".");
        pendingArenaReady = true;
        arenaReadyWaitingOnPlayersNoticeSent = false;
        pendingArenaCenter = center != null ? center.clone() : null;
        pendingStartScatter = resolveStartScatterCenter(center);
        waitingForMapNoticeSent = false;
        World world = center != null ? center.getWorld() : null;
        prepareBossArena(world);
        attemptLaunchNextCycle();
    }

    private void prepareBossArena(World world) {
        prepareBossArena(world, 0);
    }

    private void prepareBossArena(World world, int attempt) {
        if (world == null) {
            return;
        }
        if (bossSpawner.hasActiveBossRoom() || bossSpawner.isGenerationRunning()) {
            return;
        }
        if (bossSpawner.isClearRunning()) {
            if (attempt >= 10) {
                plugin.getLogger().warning("Boss room cleanup is still running; will fall back to on-demand generation.");
                return;
            }
            long delay = 40L * (attempt + 1);
            Bukkit.getScheduler().runTaskLater(plugin, () -> prepareBossArena(world, attempt + 1), delay);
            return;
        }
        Location bossCenter = config.resolveBossArenaCenter(world);
        bossSpawner.generateBossRoom(bossCenter,
                success -> plugin.getLogger().info("Boss arena primed: " + success),
                error -> {
                    plugin.getLogger().warning("Boss arena prep failed: " + error);
                    if (attempt < 3) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> prepareBossArena(world, attempt + 1), 100L);
                    }
                }
        );
    }

    private void onBossRoomClearedForNextCycle() {
        awaitingBossRoomClear = false;
        if (!loopActive) {
            return;
        }
        Bukkit.broadcastMessage(ChatColor.YELLOW + "Boss room cleared. Assembling the next arena...");
        generateCombatMap();
        attemptLaunchNextCycle();
    }

    private void attemptLaunchNextCycle() {
        if (!loopActive) {
            return;
        }
        if (!playersConfirmedNextCycle) {
            if (pendingArenaReady && !arenaReadyWaitingOnPlayersNoticeSent && readyStage == ReadyStage.POST_BOSS) {
                Bukkit.broadcastMessage(ChatColor.YELLOW + "Arena prepared. Use /ready when everyone is done shopping.");
                arenaReadyWaitingOnPlayersNoticeSent = true;
            }
            return;
        }
        if (awaitingBossRoomClear) {
            if (!waitingForMapNoticeSent) {
                Bukkit.broadcastMessage(ChatColor.YELLOW + "Boss room cleanup still running. We'll move once it's finished.");
                waitingForMapNoticeSent = true;
            }
            return;
        }
        if (!pendingArenaReady) {
            if (!waitingForMapNoticeSent) {
                Bukkit.broadcastMessage(ChatColor.YELLOW + "Arena is still generating. You'll be teleported once it's ready.");
                waitingForMapNoticeSent = true;
            }
            return;
        }
        Location targetCenter = pendingStartScatter != null ? pendingStartScatter.clone() : resolveStartScatterCenter(pendingArenaCenter);
        if (targetCenter == null) {
            Bukkit.broadcastMessage(ChatColor.RED + "Unable to locate a valid starting position for the next arena. Please contact staff.");
            return;
        }
        if (game.getPerkShopManager() != null) {
            game.getPerkShopManager().closeShop();
        }
        teleportPlayersToStart(targetCenter);
        playersConfirmedNextCycle = false;
        pendingArenaReady = false;
        waitingForMapNoticeSent = false;
        arenaReadyWaitingOnPlayersNoticeSent = false;
        pendingArenaCenter = null;
        pendingStartScatter = null;
        bossPrepNoticeActive = false;
        World world = targetCenter != null ? targetCenter.getWorld() : resolveWorld();
        waitForBossRoomThenStartCombat(world);
    }

    private void waitForBossRoomThenStartCombat(World world) {
        if (!loopActive) {
            bossPrepNoticeActive = false;
            return;
        }
        if (world == null) {
            failAndShutdown("World missing while waiting for boss arena.");
            return;
        }
        if (isBossArenaPrepared()) {
            bossPrepNoticeActive = false;
            enterWavePhase();
            return;
        }
        if (!bossPrepNoticeActive) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "Boss arena is preparing. Waves will start once it is ready.");
            bossPrepNoticeActive = true;
        }
        if (!bossSpawner.isGenerationRunning() && !bossSpawner.isClearRunning()) {
            prepareBossArena(world);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> waitForBossRoomThenStartCombat(world), BOSS_ROOM_READY_POLL_TICKS);
    }

    private void enterWavePhase() {
        if (!loopActive) {
            return;
        }
        phase = GameLoopPhase.WAVES_ACTIVE;
        wavesClearedInCycle = 0;
        cycleIndex = Math.max(0, cycleIndex);
        bossPrepNoticeActive = false;
        waveManager.triggerNextWave();
    }

    private boolean isBossArenaPrepared() {
        return bossSpawner.hasActiveBossRoom()
                && !bossSpawner.isGenerationRunning()
                && !bossSpawner.isClearRunning();
    }

    private void beginBossCountdown() {
        cancelNextWaveTask();
        if (phase != GameLoopPhase.WAVES_ACTIVE) {
            return;
        }
        phase = GameLoopPhase.BOSS_TRANSITION;
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "All waves cleared! Preparing the boss arena...");
        cancelBossCountdownTask();
        bossCountdownTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            bossCountdownTask = null;
            if (!loopActive) {
                return;
            }
            beginBossTransition();
        }, FINAL_WAVE_HOLD_TICKS);
    }

    private void beginBossTransition() {
        cancelBossCountdownTask();
        if (!loopActive) {
            return;
        }
        World world = resolveWorld();
        if (world == null) {
            failAndShutdown("World missing for boss transition.");
            return;
        }
        phase = GameLoopPhase.BOSS_TRANSITION;
        Location holdPoint = resolveMapCenter(world);
        teleportPlayersExact(holdPoint);
        applyFallDamageProtection(getOnlineParticipants(), FALL_IMMUNITY_TICKS);
        Bukkit.broadcastMessage(ChatColor.YELLOW + "Clearing arena... hold steady near the center!");
        if (mapManager.hasActiveMap()) {
            mapManager.clearMapAsync(
                    removed -> Bukkit.getScheduler().runTask(plugin, () -> launchBossEncounter(world)),
                    error -> Bukkit.getScheduler().runTask(plugin, () -> failAndShutdown(error))
            );
        } else {
            launchBossEncounter(world);
        }
    }

    private void generateBossArena(World world) {
        Location bossCenter = config.resolveBossArenaCenter(world);
        bossSpawner.generateBossRoom(bossCenter,
                success -> Bukkit.getScheduler().runTask(plugin, () -> onBossRoomReady(world)),
                error -> Bukkit.getScheduler().runTask(plugin, () -> failAndShutdown(error))
        );
    }

    private void launchBossEncounter(World world) {
        if (world == null) {
            failAndShutdown("World missing for boss encounter.");
            return;
        }
        if (bossSpawner.isGenerationRunning()) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "Finalizing the boss structure...");
            Bukkit.getScheduler().runTaskLater(plugin, () -> launchBossEncounter(world), BOSS_ROOM_READY_POLL_TICKS);
            return;
        }
        if (!bossSpawner.hasActiveBossRoom()) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "Boss arena not ready. Building now...");
            generateBossArena(world);
            return;
        }
        onBossRoomReady(world);
    }

    private void onBossRoomReady(World world) {
        if (!loopActive) {
            return;
        }
        phase = GameLoopPhase.BOSS_FIGHT;
        Location spawn = config.resolveBossSpawn(world);
        setBossSpectatorMode(false);
        if (!waveManager.startBossEncounter(spawn)) {
            failAndShutdown("Failed to start boss encounter.");
        }
        Main.game.onBossStart();
    }

    private void handleBossDefeated() {
        cancelNextWaveTask();
        if (!loopActive) {
            return;
        }
        World world = resolveWorld();
        if (world == null) {
            failAndShutdown("World missing for post-boss handling.");
            return;
        }
        phase = GameLoopPhase.POST_BOSS_READY;
        teleportPlayersExact(config.resolvePerkHub(world));
        Main.game.onBossEnd();

        if (game.getPerkShopManager() != null) {
            game.getPerkShopManager().openShopForPlayers(game.PLAYER_LIST.values());
        }
        Bukkit.broadcastMessage(ChatColor.GOLD + "Boss defeated! Buy perks, then use /ready to continue.");
        readyStage = ReadyStage.POST_BOSS;
        prepareNextCycleLaunchState(false);
        activeReadyCheck = new ReadyCheck(toUuidList(getOnlineParticipants()), ReadyCheck.Mode.CONFIRM_ONLY);
        if (activeReadyCheck.totalParticipants() == 0) {
            activeReadyCheck = null;
            onReadyCheckComplete(ReadyCheck.Mode.CONFIRM_ONLY);
        } else {
            sendPostBossPrompts();
        }
        awaitingBossRoomClear = true;
        bossSpawner.clearBossRoom(
                msg -> Bukkit.getScheduler().runTask(plugin, this::onBossRoomClearedForNextCycle),
                error -> Bukkit.getScheduler().runTask(plugin, () -> Bukkit.broadcastMessage(ChatColor.YELLOW + "Boss room cleanup pending: " + error))
        );
    }

    private void scheduleNextWave() {
        cancelNextWaveTask();
        scheduledWaveTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            scheduledWaveTask = null;
            if (loopActive && phase == GameLoopPhase.WAVES_ACTIVE) {
                waveManager.triggerNextWave();
            }
        }, NEXT_WAVE_DELAY_TICKS);
    }

    private void sendInitialReadyPrompt(Player player) {
        Component prompt = Component.text("Ready to start? ", NamedTextColor.GOLD)
                .append(Component.text("[Yes]", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/ready yes"))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to ready up", NamedTextColor.GREEN))))
                .append(Component.text(" "))
                .append(Component.text("[No]", NamedTextColor.RED, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/ready no"))
                        .hoverEvent(HoverEvent.showText(Component.text("Click if you need downtime", NamedTextColor.RED))));
        player.sendMessage(prompt);
        player.sendMessage(Component.text("This prompt is only visible to you.", NamedTextColor.GRAY));
    }

    private void sendPostBossPrompts() {
        Component prompt = Component.text("Use /ready when everyone is finished shopping.", NamedTextColor.GOLD);
        for (Player player : getOnlineParticipants()) {
            player.sendMessage(prompt);
        }
    }

    private void sendWaveTitle(int waveNumber, int enemyCount) {
        Component title = Component.text("Wave " + waveNumber, NamedTextColor.GOLD);
        Component subtitle = Component.text(Math.max(1, enemyCount) + " enemies have spawned. Defeat them!", NamedTextColor.GRAY);
        Title waveTitle = Title.title(title, subtitle, STANDARD_TITLE_TIMES);
        showTitleToPlayers(waveTitle);
    }

    private void sendWaveClearedTitle(int waveNumber) {
        Component title = Component.text("Wave " + waveNumber + " Cleared", NamedTextColor.AQUA);
        Component subtitle;
        if (waveNumber >= WAVES_PER_CYCLE) {
            subtitle = Component.text("Boss fight is next!", NamedTextColor.GRAY);
        } else {
            subtitle = Component.text("Prepare for the next wave.", NamedTextColor.GRAY);
        }
        Title clearTitle = Title.title(title, subtitle, STANDARD_TITLE_TIMES);
        showTitleToPlayers(clearTitle);
    }

    private void sendBossTitle() {
        Component title = Component.text("Boss Approaches", NamedTextColor.DARK_RED);
        Component subtitle = Component.text("Stay alive while the arena seals.", NamedTextColor.GRAY);
        Title bossTitle = Title.title(title, subtitle, STANDARD_TITLE_TIMES);
        showTitleToPlayers(bossTitle);
    }

    private void showTitleToPlayers(Title title) {
        for (Player player : getOnlineParticipants()) {
            player.showTitle(title);
        }
    }

    private Location resolveStartScatterCenter(Location defaultCenter) {
        Location candidate = defaultCenter != null ? defaultCenter.clone() : null;
        if (candidate == null) {
            candidate = mapManager.getActiveCenterLocation().map(Location::clone).orElse(null);
        }
        if (candidate == null) {
            World fallbackWorld = resolveWorld();
            if (fallbackWorld == null) {
                return null;
            }
            candidate = config.resolvePlayMapCenter(fallbackWorld);
        }
        return candidate;
    }

    private Location resolveMapCenter(World world) {
        if (world == null) {
            return null;
        }
        Location active = mapManager.getActiveCenterLocation().map(Location::clone).orElse(null);
        return active != null ? active : config.resolvePlayMapCenter(world);
    }

    private void teleportPlayersToStart(Location center) {
        List<Player> players = getOnlineParticipants();
        if (players.isEmpty() || center == null) {
            return;
        }
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        Location mapCenter = mapManager.getActiveCenterLocation().map(Location::clone).orElse(center.clone());
        if (mapCenter.getWorld() == null) {
            mapCenter.setWorld(world);
        }
        int generatedRadius = Math.max(0, mapManager.getLastRadius());
        double borderBuffer = 0.0;
        if (generatedRadius > 0) {
            borderBuffer = Math.max(SPAWN_BORDER_BUFFER, generatedRadius * 0.2);
            borderBuffer = Math.min(borderBuffer, Math.max(4.0, generatedRadius - 4.0));
        }
        double usableRadius = generatedRadius > 0
                ? Math.max(4.0, generatedRadius - borderBuffer)
                : Math.max(4.0, config.getStartScatterRadius());
        Set<Long> usedColumns = new HashSet<>();
        for (Player player : players) {
            Location safeLocation = pickRandomSpawn(world, mapCenter, usableRadius, usedColumns);
            if (safeLocation == null) {
                safeLocation = center.clone();
                int fallbackY = world.getHighestBlockYAt(safeLocation.getBlockX(), safeLocation.getBlockZ());
                safeLocation.setY(fallbackY + 1.0);
            }
            safeLocation.setYaw(random.nextFloat() * 360f);
            safeLocation.setPitch(0f);
            player.teleport(safeLocation);
        }
    }

    private Location findSafeLandingLocation(World world, double x, double z) {
        if (world == null) {
            return null;
        }
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int highest = world.getHighestBlockYAt(blockX, blockZ);
        int minY = Math.max(world.getMinHeight() + 1, highest - 40);
        for (int y = highest; y >= minY; y--) {
            Block ground = world.getBlockAt(blockX, y, blockZ);
            Material type = ground.getType();
            if (!type.isSolid()) {
                continue;
            }
            if (isFoliageOrUnsafe(type) || ground.isLiquid()) {
                continue;
            }
            Block head = world.getBlockAt(blockX, y + 1, blockZ);
            Block head2 = world.getBlockAt(blockX, y + 2, blockZ);
            if (!head.isEmpty() || !head2.isEmpty()) {
                continue;
            }
            return new Location(world, blockX + 0.5, y + 1.01, blockZ + 0.5);
        }
        return null;
    }

    private Location pickRandomSpawn(World world, Location center, double radius, Set<Long> usedColumns) {
        if (world == null || center == null) {
            return null;
        }
        double clampedRadius = Math.max(0.0, radius);
        for (int attempt = 0; attempt < MAX_SPAWN_ATTEMPTS; attempt++) {
            double distance = clampedRadius <= 0.0 ? 0.0 : Math.sqrt(random.nextDouble()) * clampedRadius;
            double angle = random.nextDouble() * Math.PI * 2.0;
            double x = center.getX() + Math.cos(angle) * distance;
            double z = center.getZ() + Math.sin(angle) * distance;
            int blockX = (int) Math.floor(x);
            int blockZ = (int) Math.floor(z);
            long key = packColumnKey(blockX, blockZ);
            if (usedColumns.contains(key)) {
                continue;
            }
            Location safeLocation = findSafeLandingLocation(world, x, z);
            if (safeLocation == null) {
                continue;
            }
            usedColumns.add(key);
            return safeLocation;
        }
        return null;
    }

    private boolean isFoliageOrUnsafe(Material material) {
        if (material == null) {
            return true;
        }
        String name = material.name();
        if (name.contains("LEAVES") || name.contains("LOG") || name.contains("WOOD")) {
            return true;
        }
        return material == Material.BARRIER || material == Material.LIGHT || material.name().contains("GLASS");
    }

    private void teleportPlayersExact(Location location) {
        List<Player> players = getOnlineParticipants();
        if (location == null) {
            return;
        }
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            Location target = location.clone().add(randomOffset(i));
            player.teleport(target);
        }
    }

    private void applyFallDamageProtection(List<Player> players, int durationTicks) {
        if (players.isEmpty()) {
            return;
        }
        for (Player player : players) {
            if (player == null) {
                continue;
            }
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, durationTicks, 1, false, false, false));
//            player.addPotionEffect(new PotionEffect(PotionEffectType., durationTicks, 4, false, false, false));
            player.setFallDistance(0f);
        }
    }

    private long packColumnKey(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xffffffffL);
    }

    private Vector randomOffset(int seed) {
        double spread = 1.5;
        double angle = (seed * 37) % 360;
        double radians = Math.toRadians(angle);
        return new Vector(Math.cos(radians) * spread, 0, Math.sin(radians) * spread);
    }

    private void enableWaveControl() {
        if (waveControlEnabled) {
            return;
        }
        waveManager.setLifecycleListener(this);
        waveManager.setAutoAdvance(false);
        waveControlEnabled = true;
    }

    private void disableWaveControl() {
        if (!waveControlEnabled) {
            return;
        }
        waveManager.setLifecycleListener(null);
        waveManager.setAutoAdvance(true);
        waveControlEnabled = false;
    }

    private List<Player> getOnlineParticipants() {
        List<Player> players = new ArrayList<>();
        for (GamePlayer gamePlayer : game.PLAYER_LIST.values()) {
            Player bukkit = gamePlayer.getMinecraftPlayer();
            if (bukkit != null && bukkit.isOnline()) {
                players.add(bukkit);
            }
        }
        return players;
    }

    private List<UUID> toUuidList(List<Player> players) {
        return players.stream().map(Player::getUniqueId).collect(Collectors.toList());
    }

    private World resolveWorld() {
        if (Main.world != null) {
            return Main.world;
        }
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
    }

    private void cancelBossCountdownTask() {
        if (bossCountdownTask != null) {
            bossCountdownTask.cancel();
            bossCountdownTask = null;
        }
    }

    private void cancelNextWaveTask() {
        if (scheduledWaveTask != null) {
            scheduledWaveTask.cancel();
            scheduledWaveTask = null;
        }
    }

    private void cancelReadyTimeout() {
        if (readyTimeoutTask != null) {
            readyTimeoutTask.cancel();
            readyTimeoutTask = null;
        }
    }

    private void scheduleReadyTimeout() {
        cancelReadyTimeout();
        if (readyStage != ReadyStage.INITIAL) {
            return;
        }
        readyTimeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!loopActive) {
                return;
            }
            if (readyStage != ReadyStage.INITIAL) {
                return;
            }
            if (activeReadyCheck == null || activeReadyCheck.isComplete()) {
                return;
            }
            failAndShutdown("Ready check timed out.");
        }, INITIAL_READY_TIMEOUT_TICKS);
    }

    private void hardResetState() {
        cancelBossCountdownTask();
        cancelNextWaveTask();
        cancelReadyTimeout();
        activeReadyCheck = null;
        loopActive = false;
        wavesClearedInCycle = 0;
        cycleIndex = 0;
        phase = GameLoopPhase.IDLE;
        readyStage = ReadyStage.INITIAL;
        disableWaveControl();
        bossPrepNoticeActive = false;
        awaitingBossRoomClear = false;
        prepareNextCycleLaunchState(false);
        setBossSpectatorMode(false);
    }

    private void failAndShutdown(String reason) {
        Bukkit.broadcastMessage(ChatColor.RED + "Game loop aborted: " + reason);
        cancelReadyTimeout();
        setBossSpectatorMode(false);
        clearActiveMapOnShutdown();
        hardResetState();
        sessionInitialized = false;
        game.EndGame();
    }

    private void clearActiveMapOnShutdown() {
        if (!mapManager.hasActiveMap()) {
            return;
        }
        if (mapManager.isGenerationRunning() || mapManager.isClearingRunning()) {
            plugin.getLogger().info("Arena clear skipped; generation or clearing already in progress.");
            return;
        }
        mapManager.clearMapAsync(
                removed -> plugin.getLogger().info("Cleared arena during shutdown (" + removed + " blocks)."),
                error -> plugin.getLogger().warning("Failed to clear arena during shutdown: " + error)
        );
    }

    private void setBossSpectatorMode(boolean enable) {
        if (enable) {
            bossSpectatorIds.clear();
            for (Player player : getOnlineParticipants()) {
                if (player.getGameMode() != GameMode.SPECTATOR) {
                    bossSpectatorIds.add(player.getUniqueId());
                    player.setGameMode(GameMode.SPECTATOR);
                }
            }
            return;
        }
        if (bossSpectatorIds.isEmpty()) {
            return;
        }
        for (UUID id : new HashSet<>(bossSpectatorIds)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) {
                player.setGameMode(GameMode.ADVENTURE);
            }
            bossSpectatorIds.remove(id);
        }
    }
}
