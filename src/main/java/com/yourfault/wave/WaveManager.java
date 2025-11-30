package com.yourfault.wave;

import com.yourfault.Enemy.mob.LaserZombieEnemy;
import com.yourfault.Main;
import com.yourfault.map.MapManager;
import com.yourfault.system.Game;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;

public class WaveManager {
    private final Game game;
    private final Random random = new Random();
    private final MapManager mapManager;
    private final List<UUID> activeWaveEnemyIds = new ArrayList<>();
    private final Map<UUID, WaveEnemyInstance> activeWaveEnemies = new HashMap<>();
    private final List<WaveEnemyInstance> lastSpawnedEnemies = new ArrayList<>();

    private WaveDifficulty difficulty = WaveDifficulty.MEDIUM;
    private int currentWave = 0;
    private boolean active = false;
    private boolean waveInProgress = false;
    private boolean nextWaveScheduled = false;

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

    public void stop() {
        this.active = false;
        this.waveInProgress = false;
        this.nextWaveScheduled = false;
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
        waveInProgress = true;
        nextWaveScheduled = false;
        currentWave++;
        Bukkit.getScoreboardManager().getMainScoreboard().getTeam("21_WAVEINFO_CURRENTWAVE").suffix(Component.text(": " + currentWave));
        activeWaveEnemyIds.clear();
        activeWaveEnemies.clear();
        lastSpawnedEnemies.clear();
        WaveContext context = buildWaveContext(playersReady);
        Bukkit.broadcastMessage(ChatColor.AQUA + "Wave " + currentWave + " incoming! Weight budget: " + String.format("%.1f", context.totalWeightBudget()));
        List<WaveEnemyType> composition = planComposition(context);
        game.showWaveTitle(currentWave, composition.size());
        spawnWave(composition, context);
        if (activeWaveEnemyIds.isEmpty()) {
            waveInProgress = false;
            Bukkit.broadcastMessage(ChatColor.RED + "Wave " + currentWave + " failed to spawn any enemies.");
        }
    }

    private WaveContext buildWaveContext(int playerCount) {
        double baseWeight = 10 + (currentWave * 2.0);
        double exponential = baseWeight * Math.pow(1.1, currentWave / 5.0);
        double playerScaling = 1 + (playerCount * 0.2);
        double totalWeight = (baseWeight + exponential) * playerScaling + difficulty.modeBonus();
        return new WaveContext(currentWave, playerCount, difficulty, baseWeight, exponential, totalWeight);
    }

    private List<WaveEnemyType> planComposition(WaveContext context) {
        double remaining = context.totalWeightBudget();
        double maxPerType = context.totalWeightBudget() * 0.5;
        Map<WaveEnemyType, Double> weightUsed = new EnumMap<>(WaveEnemyType.class);
        List<WaveEnemyType> result = new ArrayList<>();

        if (context.waveNumber() % 10 == 0) {
            result.add(WaveEnemyType.BOSS);
            double bossWeight = WaveEnemyType.BOSS.weight();
            weightUsed.put(WaveEnemyType.BOSS, bossWeight);
            remaining = Math.max(0, remaining - bossWeight);
        }

        while (remaining >= getCheapestWeight()) {
            List<WaveEnemyType> candidates = new ArrayList<>();
            for (WaveEnemyType type : getAffordableTypes(context, remaining)) {
                if (weightUsed.getOrDefault(type, 0.0) + type.weight() <= maxPerType) {
                    candidates.add(type);
                }
            }
            if (candidates.isEmpty()) {
                break;
            }
            WaveEnemyType choice = chooseRandomType(candidates);
            result.add(choice);
            weightUsed.merge(choice, choice.weight(), Double::sum);
            remaining -= choice.weight();
        }

        return result;
    }

    private double getCheapestWeight() {
        double min = Double.MAX_VALUE;
        for (WaveEnemyType type : WaveEnemyType.values()) {
            min = Math.min(min, type.weight());
        }
        return min == Double.MAX_VALUE ? 1.0 : min;
    }

    private List<WaveEnemyType> getAffordableTypes(WaveContext context, double remaining) {
        int allowedTier = Math.max(1, (context.waveNumber() / 5) + 1);
        List<WaveEnemyType> list = new ArrayList<>();
        for (WaveEnemyType type : WaveEnemyType.values()) {
            if (type.weight() > remaining) continue;
            if (context.waveNumber() < type.minWave()) continue;
            if (type.tier() > allowedTier && !type.isBoss()) continue;
            if (type.isBoss() && context.waveNumber() % 10 != 0) continue;
            list.add(type);
        }
        return list;
    }

    private WaveEnemyType chooseRandomType(List<WaveEnemyType> candidates) {
        double totalBias = candidates.stream().mapToDouble(WaveEnemyType::spawnBias).sum();
        double roll = random.nextDouble() * totalBias;
        double cumulative = 0.0;
        for (WaveEnemyType candidate : candidates) {
            cumulative += candidate.spawnBias();
            if (roll <= cumulative) {
                return candidate;
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    private void spawnWave(List<WaveEnemyType> composition, WaveContext context) {
        if (composition.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "No enemies could be spawned for wave " + context.waveNumber());
            return;
        }
        World world = Main.world;
        if (world == null) {
            Bukkit.broadcastMessage(ChatColor.RED + "World is not ready. Cannot spawn wave.");
            return;
        }
        for (WaveEnemyType type : composition) {
            Location spawnLocation = pickSpawnLocation();
            if (spawnLocation == null) {
                continue;
            }
            LivingEntity entity = type.spawn(world, spawnLocation);
            tagWaveEntity(entity, type);
            applyScaling(entity, type, context);
            activeWaveEnemyIds.add(entity.getUniqueId());
            Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "Spawned " + type.name() + " for wave " + context.waveNumber());
        }
    }

    private Location pickSpawnLocation() {
        List<Location> markers = mapManager != null ? mapManager.getSpawnMarkers() : Collections.emptyList();
        if (!markers.isEmpty()) {
            List<Location> nearbyMarkers = markersNearActivePlayers(markers, 5.0);
            Location choice = nearbyMarkers.isEmpty() ? markers.get(random.nextInt(markers.size())) : nearbyMarkers.get(random.nextInt(nearbyMarkers.size()));
            Location spawn = choice.clone();
            spawn.setY(spawn.getY() + 1);
            return spawn;
        }
        return pickFallbackLocation();
    }

    private List<Location> markersNearActivePlayers(List<Location> markers, double range) {
        double rangeSquared = range * range;
        List<Location> candidates = new ArrayList<>();
        List<Location> playerLocations = getActivePlayerLocations();
        for (Location marker : markers) {
            for (Location playerLocation : playerLocations) {
                if (!Objects.equals(marker.getWorld(), playerLocation.getWorld())) {
                    continue;
                }
                double dx = marker.getX() - playerLocation.getX();
                double dz = marker.getZ() - playerLocation.getZ();
                if ((dx * dx) + (dz * dz) <= rangeSquared) {
                    candidates.add(marker);
                    break;
                }
            }
        }
        return candidates;
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

    private Location pickFallbackLocation() {
        if (Main.world == null || Main.world.getPlayers().isEmpty()) {
            return null;
        }
        Player target = Main.world.getPlayers().get(random.nextInt(Main.world.getPlayers().size()));
        if (target == null) {
            return null;
        }
        Location base = target.getLocation();
        if (base == null) {
            return null;
        }
        double offsetX = random.nextInt(14) - 7;
        double offsetZ = random.nextInt(14) - 7;
        Location spawn = base.clone().add(offsetX, 0, offsetZ);
        spawn.setY(Main.world.getHighestBlockYAt(spawn) + 1);
        return spawn;
    }

    private void applyScaling(LivingEntity entity, WaveEnemyType type, WaveContext context) {
        double baseHealth = type.baseHealth() * (1 + (context.waveNumber() * 0.15));
        double healthMultiplier = 1 + (type.weight() * 0.05);
        double finalHealth = baseHealth * healthMultiplier * context.difficulty().difficultyScale();

        double baseDefense = (context.waveNumber() * 0.3) + (type.weight() * 0.2);
        double finalDefense = baseDefense * context.difficulty().difficultyScale();

        double finalDamage = type.baseDamage() * (1 + (context.waveNumber() * 0.1));

        double newMax = Math.max(finalHealth, 1.0);
        if (entity.getMaxHealth() != newMax) {
            entity.setMaxHealth(newMax);
        }
        entity.setHealth(Math.max(1.0, newMax));

        WaveEnemyInstance waveEnemy;
        if (type == WaveEnemyType.LASER_ZOMBIE) {
            waveEnemy = new LaserZombieEnemy(
                    entity,
                    (float) finalHealth,
                    (float) finalHealth,
                    (float) finalDefense,
                    type,
                    finalDamage
            );
        } else {
            waveEnemy = new WaveEnemyInstance(
                    entity,
                    (float) finalHealth,
                    (float) finalHealth,
                    (float) finalDefense,
                    type,
                    finalDamage
            );
        }
        lastSpawnedEnemies.add(waveEnemy);
        activeWaveEnemies.put(entity.getUniqueId(), waveEnemy);

    }

    public List<UUID> getActiveWaveEnemyIds() {
        return new ArrayList<>(activeWaveEnemyIds);
    }

    public List<WaveEnemyInstance> getLastSpawnedEnemies() {
        return new ArrayList<>(lastSpawnedEnemies);
    }

    public WaveEnemyInstance getActiveEnemy(UUID uuid) {
        return activeWaveEnemies.get(uuid);
    }

    public void handleEnemyHit(UUID enemyId, GamePlayer attacker) {
        if (!active || attacker == null) {
            return;
        }
        WaveEnemyInstance instance = activeWaveEnemies.get(enemyId);
        if (instance == null) {
            return;
        }
        rewardPlayer(attacker, instance.getType().hitCoins(), instance.getType().hitXp());
    }

    public void handleEnemyDeath(UUID enemyId, GamePlayer killer) {
        WaveEnemyInstance instance = activeWaveEnemies.remove(enemyId);
        activeWaveEnemyIds.remove(enemyId);
        if (!active || instance == null) {
            return;
        }
        rewardPlayer(killer, instance.getType().killCoins(), instance.getType().killXp());
        checkWaveCompletion();
    }

    private void rewardPlayer(GamePlayer player, int coins, int xp) {
        if (player == null) {
            return;
        }
        if (coins > 0) {
            player.addCoins(coins);
        }
        if (xp > 0) {
            player.addExperience(xp);
        }
    }

    private void checkWaveCompletion() {
        if (!waveInProgress) {
            return;
        }
        if (!activeWaveEnemies.isEmpty()) {
            return;
        }
        waveInProgress = false;
        Bukkit.broadcastMessage(ChatColor.GREEN + "Wave " + currentWave + " cleared! Next wave in 5 seconds.");
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
            WaveEnemyInstance instance = activeWaveEnemies.get(enemyId);
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
            WaveEnemyInstance instance = activeWaveEnemies.get(enemyId);
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
        return removed;
    }

    private void tagWaveEntity(LivingEntity entity, WaveEnemyType type) {
        entity.addScoreboardTag("lot_wave_enemy");
        entity.addScoreboardTag("lot_wave_enemy_" + type.name().toLowerCase(Locale.ROOT));
    }

    public WaveEnemyInstance spawnEnemyAt(Location location, WaveEnemyType type, boolean contributeToWave) {
        if (location == null || location.getWorld() == null || type == null) {
            return null;
        }
        WaveContext context = buildWaveContext(Math.max(1, game.PLAYER_LIST.size()));
        LivingEntity entity = type.spawn(location.getWorld(), location.clone());
        tagWaveEntity(entity, type);
        applyScaling(entity, type, context);
        if (contributeToWave) {
            activeWaveEnemyIds.add(entity.getUniqueId());
        }
        return activeWaveEnemies.get(entity.getUniqueId());
    }

    private void scheduleAutoAdvance() {
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

}
