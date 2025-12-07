package com.yourfault.wave;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.*;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.Main;
import com.yourfault.map.MapManager;
import com.yourfault.system.Game;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.system.TabInfo;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class WaveManager {
    private final Game game;
    private final MapManager mapManager;
    private final Random random = new Random();
    private static final double MIN_SPAWN_RADIUS = 15.0;
    private static final double SPAWN_RADIUS_VARIANCE = 8.0;
    private static final int MAX_SPAWN_ATTEMPTS = 40;
    private static final int FALLBACK_ATTEMPTS = 20;
    private final List<UUID> activeWaveEnemyIds = new ArrayList<>();
    private final Map<UUID, Enemy> activeWaveEnemies = new HashMap<>();
    private final List<Enemy> lastSpawnedEnemies = new ArrayList<>();

    private WaveDifficulty difficulty = WaveDifficulty.MEDIUM;
    private int currentWave = 0;
    private boolean active = false;
    private boolean waveInProgress = false;
    private boolean nextWaveScheduled = false;
    private boolean autoAdvanceEnabled = true;
    private boolean bossEncounterActive = false;
    private WaveLifecycleListener lifecycleListener;
    public int currentWaveEnemyCount = 0;

    public final HashMap<String,AbstractEnemyType> EnemyTypes = new HashMap<>(){{
        put("archer",new Archer_Type());
        put("laserzombie",new LaserZombie_Type());
        put("boss",new Boss_Type());
        put("brute",new Brute_Type());
        put("grunt",new Grunt_Type());
        put("mage",new Mage_Type());
        put("spinny",new Spinny_Type());
    }};

    public WaveManager(Game game, MapManager mapManager) {
        this.game = game;
        this.mapManager = mapManager;
    }

    public void initializeSession(WaveDifficulty difficulty) {
        this.difficulty = difficulty;
        this.currentWave = 0;
        this.active = true;
        this.waveInProgress = false;
        this.nextWaveScheduled = false;
        this.bossEncounterActive = false;
        activeWaveEnemyIds.clear();
        activeWaveEnemyIds.clear();
        lastSpawnedEnemies.clear();
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public void overrideCurrentWaveCounter(int waveNumber) {
        this.currentWave = Math.max(0, waveNumber);
    }

    public boolean isActive() {
        return active;
    }

    public WaveDifficulty getDifficulty() {
        return difficulty;
    }

    public void setLifecycleListener(WaveLifecycleListener lifecycleListener) {
        this.lifecycleListener = lifecycleListener;
    }

    public void setAutoAdvance(boolean enabled) {
        this.autoAdvanceEnabled = enabled;
    }

    public void stop() {
        this.active = false;
        this.waveInProgress = false;
        this.nextWaveScheduled = false;
        this.bossEncounterActive = false;
        activeWaveEnemyIds.clear();
        activeWaveEnemies.clear();
        lastSpawnedEnemies.clear();

    }

    public void triggerNextWave() {

        if (!active) {
            Bukkit.broadcastMessage(ChatColor.RED + "Wave manager is not active.");
            return;
        }
        if (waveInProgress) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "Wave " + currentWave + " is still in progress.");
            return;
        }
        int playersReady = game.PLAYER_LIST.size();
        if (playersReady == 0) {
            Bukkit.broadcastMessage(ChatColor.RED + "No players have joined the game. Wave cancelled.");
            return;
        }

        bossEncounterActive = false;
        waveInProgress = true;
        nextWaveScheduled = false;
        currentWave++;
        updateWaveInfoSuffix(": " + currentWave);
        activeWaveEnemyIds.clear();
        activeWaveEnemies.clear();
        lastSpawnedEnemies.clear();
        WaveContext context = buildWaveContext(playersReady);

        Bukkit.broadcastMessage(ChatColor.AQUA + "Wave " + currentWave + " incoming! Weight budget: " + String.format("%.1f", context.totalWeightBudget()));
        List<AbstractEnemyType> composition = planComposition(context);
        game.showWaveTitle(currentWave, composition.size());

        spawnWave(composition, context);
        notifyEncounterStarted(currentWave, WaveEncounterType.STANDARD, composition.size());
        if (activeWaveEnemyIds.isEmpty()) {
            waveInProgress = false;
            Bukkit.broadcastMessage(ChatColor.RED + "Wave " + currentWave + " failed to spawn any enemies.");
            notifyEncounterCompleted(currentWave, WaveEncounterType.STANDARD);
        }
    }

    private WaveContext buildWaveContext(int playerCount) {
        double baseWeight = 10 + (currentWave * 2.0);
        double exponential = baseWeight * Math.pow(1.1, currentWave / 5.0);
        double playerScaling = 1 + (playerCount * 0.2);
        double totalWeight = (baseWeight + exponential) * playerScaling + difficulty.modeBonus();
        return new WaveContext(currentWave, playerCount, difficulty, baseWeight, exponential, totalWeight);
    }

    private List<AbstractEnemyType> planComposition(WaveContext context) {
        double remaining = context.totalWeightBudget();
        double maxPerType = context.totalWeightBudget() * 0.5;
        Map<AbstractEnemyType, Double> weightUsed = new HashMap<>();
        List<AbstractEnemyType> result = new ArrayList<>();

        while (remaining >= getCheapestWeight()) {
            List<AbstractEnemyType> candidates = new ArrayList<>();
            for (AbstractEnemyType type : getAffordableTypes(context, remaining)) {
                if (weightUsed.getOrDefault(type, 0.0) + type.weight <= maxPerType) {
                    candidates.add(type);
                }
            }
            if (candidates.isEmpty()) {
                break;
            }
            AbstractEnemyType choice = chooseRandomType(candidates);
            result.add(choice);
            weightUsed.merge(choice, (double)choice.weight, Double::sum);
            remaining -= choice.weight;
        }

        return result;
    }

    private double getCheapestWeight() {
        double min = Double.MAX_VALUE;
        for (AbstractEnemyType type : EnemyTypes.values()) {
            min = Math.min(min, type.weight);
        }
        return min == Double.MAX_VALUE ? 1.0 : min;
    }

    private List<AbstractEnemyType> getAffordableTypes(WaveContext context, double remaining) {
        int allowedTier = Math.max(1, (context.waveNumber() / 5) + 1);
        List<AbstractEnemyType> list = new ArrayList<>();
        for (AbstractEnemyType type :  EnemyTypes.values()) {
            if (type.isBoss) {
                continue;
            }
            if (type.weight > remaining) continue;
            if (context.waveNumber() < type.minWave) continue;
            if (type.tier > allowedTier) continue;
            list.add(type);
        }
        return list;
    }

    private AbstractEnemyType chooseRandomType(List<AbstractEnemyType> candidates) {
        double totalBias = candidates.stream().mapToDouble(x -> x.spawnBias).sum();
        double roll = random.nextDouble() * totalBias;
        double cumulative = 0.0;
        for (AbstractEnemyType candidate : candidates) {
            cumulative += candidate.spawnBias;
            if (roll <= cumulative) {
                return candidate;
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    private void spawnWave(List<AbstractEnemyType> composition, WaveContext context) {
        if (composition.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "No enemies could be spawned for wave " + context.waveNumber());
            return;
        }
        World world = Main.world;
        if (world == null) {
            Bukkit.broadcastMessage(ChatColor.RED + "World is not ready. Cannot spawn wave.");
            return;
        }
        Set<String> usedSpawnBlocks = new HashSet<>();
        Map<AbstractEnemyType, Integer> spawnCounts = new LinkedHashMap<>();
        for (AbstractEnemyType type : composition) {
            Location spawnLocation = pickSpawnLocation(usedSpawnBlocks);
            if (spawnLocation == null) {
                continue;
            }
            spawnEnemyAt(spawnLocation, type, context, true);
            spawnCounts.merge(type, 1, Integer::sum);
        }
        if (!spawnCounts.isEmpty()) {
            broadcastSpawnSummary(spawnCounts);
            currentWaveEnemyCount = spawnCounts.values().stream().mapToInt(Integer::intValue).sum();
        }
        Main.game.onNextWave(currentWave);

    }

    private Location pickSpawnLocation(Set<String> usedBlocks) {
        List<Location> playerLocations = getActivePlayerLocations();
        if (playerLocations.isEmpty()) {
            playerLocations = getWorldPlayerLocations();
        }
        Location candidate = tryPickLocationNearPlayers(playerLocations, usedBlocks);
        return candidate != null ? candidate : pickWorldSpawnFallback(usedBlocks);
    }

    private Location tryPickLocationNearPlayers(List<Location> playerLocations, Set<String> usedBlocks) {
        if (playerLocations.isEmpty()) {
            return null;
        }
        for (int attempt = 0; attempt < MAX_SPAWN_ATTEMPTS; attempt++) {
            Location reference = playerLocations.get(random.nextInt(playerLocations.size()));
            Location seed = randomLocationAround(reference, MIN_SPAWN_RADIUS, MIN_SPAWN_RADIUS + SPAWN_RADIUS_VARIANCE);
            if (seed == null) {
                continue;
            }
            Location spawn = finalizeSpawnLocation(seed);
            if (spawn == null) {
                continue;
            }
            if (!isFarEnoughFromPlayers(spawn, playerLocations)) {
                continue;
            }
            if (!usedBlocks.add(locationKey(spawn))) {
                continue;
            }
            return spawn;
        }
        return null;
    }

    private Location randomLocationAround(Location origin, double minDistance, double maxDistance) {
        if (origin == null || origin.getWorld() == null) {
            return null;
        }
        double span = Math.max(0.0, maxDistance - minDistance);
        double distance = minDistance + (span > 0 ? random.nextDouble() * span : 0.0);
        double angle = random.nextDouble() * Math.PI * 2;
        double offsetX = Math.cos(angle) * distance;
        double offsetZ = Math.sin(angle) * distance;
        Location candidate = origin.clone().add(offsetX, 0, offsetZ);
        candidate.setY(origin.getY());
        return candidate;
    }

    private boolean isFarEnoughFromPlayers(Location candidate, List<Location> playerLocations) {
        double minDistanceSquared = MIN_SPAWN_RADIUS * MIN_SPAWN_RADIUS;
        for (Location playerLocation : playerLocations) {
            if (playerLocation == null) {
                continue;
            }
            if (!Objects.equals(candidate.getWorld(), playerLocation.getWorld())) {
                continue;
            }
            double dx = candidate.getX() - playerLocation.getX();
            double dz = candidate.getZ() - playerLocation.getZ();
            if ((dx * dx) + (dz * dz) < minDistanceSquared) {
                return false;
            }
        }
        return true;
    }

    private Location pickWorldSpawnFallback(Set<String> usedBlocks) {
        if (Main.world == null) {
            return null;
        }
        for (int attempt = 0; attempt < FALLBACK_ATTEMPTS; attempt++) {
            Location seed = Main.world.getSpawnLocation().clone();
            double offsetX = random.nextInt(41) - 20;
            double offsetZ = random.nextInt(41) - 20;
            seed.add(offsetX, 0, offsetZ);
            Location spawn = finalizeSpawnLocation(seed);
            if (spawn == null) {
                continue;
            }
            if (!usedBlocks.add(locationKey(spawn))) {
                continue;
            }
            return spawn;
        }
        Location fallback = finalizeSpawnLocation(Main.world.getSpawnLocation().clone());
        if (fallback != null) {
            usedBlocks.add(locationKey(fallback));
        }
        return fallback;
    }

    private Location finalizeSpawnLocation(Location seed) {
        if (seed == null || seed.getWorld() == null) {
            return null;
        }
        if (!isWithinActiveArena(seed)) {
            return null;
        }
        Location grounded = snapToGround(seed);
        if (grounded == null) {
            return null;
        }
        return isWithinActiveArena(grounded) ? grounded : null;
    }

    private Location snapToGround(Location sample) {
        if (sample == null || sample.getWorld() == null) {
            return null;
        }
        World world = sample.getWorld();
        int x = sample.getBlockX();
        int z = sample.getBlockZ();
        int highestY = world.getHighestBlockYAt(x, z);
        int minY = Math.max(world.getMinHeight(), highestY - 32);
        for (int y = highestY; y >= minY; y--) {
            Block block = world.getBlockAt(x, y, z);
            if (!isValidGroundBlock(block)) {
                continue;
            }
            Block above = block.getRelative(0, 1, 0);
            Block aboveTwo = block.getRelative(0, 2, 0);
            if (above.getType().isSolid() || aboveTwo.getType().isSolid()) {
                continue;
            }
            return new Location(world, x + 0.5, block.getY() + 1.01, z + 0.5);
        }
        return null;
    }

    private boolean isValidGroundBlock(Block block) {
        if (block == null) {
            return false;
        }
        Material type = block.getType();
        if (type.isAir() || block.isLiquid()) {
            return false;
        }
        if (!type.isSolid()) {
            return false;
        }
        String name = type.name();
        if (name.contains("LEAVES") || name.contains("LOG") || name.contains("WOOD")) {
            return false;
        }
        if (name.contains("FENCE") || name.contains("WALL")) {
            return false;
        }
        return switch (type) {
            case BARRIER, BEDROCK, MAGMA_BLOCK, CAMPFIRE, SOUL_CAMPFIRE, CACTUS, COBWEB -> false;
            default -> true;
        };
    }

    private boolean isWithinActiveArena(Location location) {
        if (mapManager == null) {
            return true;
        }
        Optional<Location> centerOpt = mapManager.getActiveCenterLocation();
        if (centerOpt.isEmpty()) {
            return true;
        }
        Location center = centerOpt.get();
        if (!Objects.equals(center.getWorld(), location.getWorld())) {
            return false;
        }
        int radius = Math.max(0, mapManager.getLastRadius());
        if (radius <= 0) {
            return true;
        }
        double limit = Math.max(4.0, radius - 3.0);
        double dx = location.getX() - center.getX();
        double dz = location.getZ() - center.getZ();
        return (dx * dx) + (dz * dz) <= limit * limit;
    }

    private List<Location> getActivePlayerLocations() {
        List<Location> locations = new ArrayList<>();
        for (GamePlayer player : game.PLAYER_LIST.values()) {
            Player bukkitPlayer = player.getMinecraftPlayer();
            if (bukkitPlayer != null && bukkitPlayer.isOnline() && !bukkitPlayer.isDead()) {
                locations.add(bukkitPlayer.getLocation());
            }
        }
        return locations;
    }

    private List<Location> getWorldPlayerLocations() {
        if (Main.world == null) {
            return Collections.emptyList();
        }
        List<Location> locations = new ArrayList<>();
        for (Player player : Main.world.getPlayers()) {
            if (player != null && player.isOnline() && !player.isDead()) {
                locations.add(player.getLocation());
            }
        }
        return locations;
    }

    private String locationKey(Location location) {
        String worldName = location.getWorld() != null ? location.getWorld().getName() : "unknown";
        return worldName + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private void broadcastSpawnSummary(Map<AbstractEnemyType, Integer> spawnCounts) {
        int total = spawnCounts.values().stream().mapToInt(Integer::intValue).sum();
        StringBuilder summary = new StringBuilder();
        summary.append("================\n");
        summary.append("[").append(total).append("] mobs have spawned\n");
        for (Map.Entry<AbstractEnemyType, Integer> entry : spawnCounts.entrySet()) {
            summary.append(entry.getValue())
                    .append("x ")
                    .append(entry.getKey().displayName.toUpperCase(Locale.ROOT))
                    .append("\n");
        }
        summary.append("================");
        Bukkit.broadcastMessage(summary.toString());
    }

    private Enemy InstantiateWaveEnemy(Mob entity, AbstractEnemyType type, WaveContext context) {


        Enemy waveEnemy = type.CreateEnemyInstance(entity, context);
        lastSpawnedEnemies.add(waveEnemy);
        activeWaveEnemies.put(entity.getUniqueId(), waveEnemy);
        return waveEnemy;
    }

    public List<UUID> getActiveWaveEnemyIds() {
        return new ArrayList<>(activeWaveEnemyIds);
    }

    public List<Enemy> getLastSpawnedEnemies() {
        return new ArrayList<>(lastSpawnedEnemies);
    }

    public Enemy getActiveEnemy(UUID uuid) {
        return activeWaveEnemies.get(uuid);
    }

    public void handleEnemyHit(UUID enemyId, GamePlayer attacker) {

        if (!active || attacker == null) {
            return;
        }
        Enemy instance = activeWaveEnemies.get(enemyId);
        if (instance == null) {
            return;
        }
        rewardPlayer(attacker, instance.enemyType.hitCoins, instance.enemyType.hitXp);
    }

    public void handleEnemyDeath(UUID enemyId, GamePlayer killer) {
        Enemy instance = activeWaveEnemies.remove(enemyId);
        activeWaveEnemyIds.remove(enemyId);
        if (!active || instance == null) {
            return;
        }
        rewardPlayer(killer, instance.enemyType.killCoins, instance.enemyType.killXp);
        checkWaveCompletion();
    }

    private void rewardPlayer(GamePlayer player, int coins, int xp) {
        if (player == null) {
            return;
        }
        boolean rewarded = false;
        if (coins > 0) {
            player.addCoins(coins);
            rewarded = true;
        }
        if (xp > 0) {
            player.addExperience(xp);
            rewarded = true;
        }
        if (rewarded) {
            sendRewardMessage(player, coins, xp);
        }
    }

    private void sendRewardMessage(GamePlayer player, int coins, int xp) {
        Player bukkitPlayer = player.getMinecraftPlayer();
        if (bukkitPlayer == null) {
            return;
        }
        String message = String.format("+%d coins, + %d XP", coins, xp);
        bukkitPlayer.sendMessage(message);
    }

    private void checkWaveCompletion() {
        if (!waveInProgress) {
            return;
        }
        if (!activeWaveEnemies.isEmpty()) {
            return;
        }
        waveInProgress = false;
        if (bossEncounterActive) {
            bossEncounterActive = false;
            Bukkit.broadcastMessage(ChatColor.GOLD + "Boss encounter cleared!");
            notifyEncounterCompleted(currentWave, WaveEncounterType.BOSS);
            return;
        }
        Bukkit.broadcastMessage(ChatColor.GREEN + "Wave " + currentWave + " cleared! Next wave in 5 seconds.");
        notifyEncounterCompleted(currentWave, WaveEncounterType.STANDARD);
        scheduleAutoAdvance();
    }

    public void skipCurrentWave(CommandSender initiator) {
        if (!active || !waveInProgress) {
            if (initiator != null) {
                initiator.sendMessage(ChatColor.YELLOW + "No active wave to skip.");
            }
            return;
        }
        List<UUID> targets = new ArrayList<>(activeWaveEnemyIds);
        for (UUID enemyId : targets) {
            Enemy instance = activeWaveEnemies.get(enemyId);
            if (instance != null) {
                instance.Destroy();
            } else {
                LivingEntity entity = (LivingEntity) Bukkit.getEntity(enemyId);
                if (entity != null) {
                    entity.remove();
                }
                handleEnemyDeath(enemyId, null);
            }
        }
        Bukkit.broadcastMessage(ChatColor.RED + "Wave " + currentWave + " was skipped by an administrator.");
    }

    public int clearAllEnemiesInstantly(boolean suppressCompletionBroadcast) {
        List<UUID> targets = new ArrayList<>(activeWaveEnemyIds);
        if (targets.isEmpty()) {
            return 0;
        }
        if (suppressCompletionBroadcast) {
            waveInProgress = false;
        }
        int removed = 0;
        for (UUID enemyId : targets) {
            Enemy instance = activeWaveEnemies.get(enemyId);
            if (instance != null) {
                instance.Destroy();
                removed++;
                continue;
            }
            LivingEntity entity = (LivingEntity) Bukkit.getEntity(enemyId);
            if (entity != null) {
                entity.remove();
                removed++;
            }
            handleEnemyDeath(enemyId, null);
        }
        activeWaveEnemyIds.clear();
        activeWaveEnemies.clear();
        lastSpawnedEnemies.clear();
        nextWaveScheduled = false;
        bossEncounterActive = false;
        return removed;
    }


    public Enemy spawnEnemyAt(Location location,AbstractEnemyType type,WaveContext context, boolean contributeToWave)
    {
        if (location == null || location.getWorld() == null || type == null) {
            return null;
        }
        Mob entity = type.SpawnEntity(location);
        InstantiateWaveEnemy(entity, type, context);
        if (contributeToWave) {
            activeWaveEnemyIds.add(entity.getUniqueId());
        }
        return activeWaveEnemies.get(entity.getUniqueId());
    }
    public Enemy spawnEnemyAt(Location location, AbstractEnemyType type) {
        WaveContext context = buildWaveContext(Math.max(1, game.PLAYER_LIST.size()));
        return spawnEnemyAt(location, type, context, false);
    }

    public boolean startBossEncounter(Location location) {
        if (!active) {
            Bukkit.broadcastMessage(ChatColor.RED + "Wave manager is not active.");
            return false;
        }
        if (waveInProgress) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "Cannot start boss while another encounter is active.");
            return false;
        }
        if (location == null || location.getWorld() == null) {
            Bukkit.broadcastMessage(ChatColor.RED + "Boss spawn location is invalid.");
            return false;
        }
        bossEncounterActive = true;
        waveInProgress = true;
        nextWaveScheduled = false;
        activeWaveEnemyIds.clear();
        activeWaveEnemies.clear();
        lastSpawnedEnemies.clear();
//
//        WaveContext context = buildWaveContext(Math.max(1, game.PLAYER_LIST.size()));
//        LivingEntity bossEntity = WaveEnemyType.BOSS.spawn(location.getWorld(), location.clone());
//        InstantiateWaveEnemy(bossEntity, WaveEnemyType.BOSS, context);
//        activeWaveEnemyIds.add(bossEntity.getUniqueId());
        spawnEnemyAt(location,EnemyTypes.get("boss"));
        updateWaveInfoSuffix(": Boss");
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "Boss encounter beginning! Hold firm.");
        notifyEncounterStarted(currentWave, WaveEncounterType.BOSS, 1);
//        if (activeWaveEnemyIds.isEmpty()) {
//            bossEncounterActive = false;
//            waveInProgress = false;
//            notifyEncounterCompleted(currentWave, WaveEncounterType.BOSS);
//            return false;
//        }
        return true;
    }

    private void scheduleAutoAdvance() {
        if (!autoAdvanceEnabled) {
            return;
        }
        if (nextWaveScheduled) {
            return;
        }
        nextWaveScheduled = true;
        Bukkit.getScheduler().runTaskLater(Main.plugin, () -> {
            nextWaveScheduled = false;
            if (!active || waveInProgress) {
                return;
            }
            triggerNextWave();
        }, 100L);
    }

    public boolean isWaveInProgress() {
        return waveInProgress;
    }

    private void notifyEncounterStarted(int waveNumber, WaveEncounterType type, int enemyCount) {
        if (lifecycleListener != null) {
            lifecycleListener.onEncounterStarted(waveNumber, type, enemyCount);
        }
    }

    private void notifyEncounterCompleted(int waveNumber, WaveEncounterType type) {
        if (lifecycleListener != null) {
            lifecycleListener.onEncounterCompleted(waveNumber, type);
        }
    }

    private void updateWaveInfoSuffix(String suffixValue) {
        if (suffixValue == null) {
            return;
        }
        Team team = Main.tabInfo.GetTeam.get(TabInfo.TabType.WAVEINFO_CURRENTWAVE);
        if (team != null) {
            team.suffix(Component.text(suffixValue));
        }
    }

}
