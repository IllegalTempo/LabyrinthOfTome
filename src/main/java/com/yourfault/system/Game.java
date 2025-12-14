package com.yourfault.system;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Main;
import com.yourfault.gameloop.GameLoopManager;
import com.yourfault.listener.PerkSelectionListener;
import com.yourfault.map.BossStructureSpawner;
import com.yourfault.map.MapManager;
import com.yourfault.perks.perkType.*;
import com.yourfault.perks.PerkType;
import com.yourfault.perks.shop.PerkShopManager;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveDifficulty;
import com.yourfault.wave.WaveManager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Game {
    private static final Title.Times GAME_START_TITLE_TIMES = Title.Times.times(
            Duration.ofMillis(500),
            Duration.ofSeconds(2),
            Duration.ofMillis(1000)
    );
    private static final Title.Times WAVE_TITLE_TIMES = Title.Times.times(
            Duration.ofMillis(250),
            Duration.ofSeconds(2),
            Duration.ofMillis(400)
    );
    public Game(JavaPlugin plugin)
    {
        Main.game = this;
        this.plugin = plugin;
        AddExistingPlayer();
        Main_Update();
        RegisterPerks();
        RegisterPerksListener();
        perkShopManager = new PerkShopManager(this);
        InitializeTeam();
        InitialzieBossBar();
    }
    private final JavaPlugin plugin;
    private BukkitRunnable updateTask;
    public Vector Gravity = new Vector(0,-0.5,0);
    public HashMap<UUID, GamePlayer> PLAYER_LIST = new HashMap<>();
    public HashMap<UUID,Projectile> PROJECTILE_LIST = new HashMap<>();
    public HashMap<UUID, Enemy> ENEMY_LIST = new HashMap<>();
    public HashMap<UUID,GamePlayer> getDeadPlayer = new HashMap<>();
    private PerkSelectionListener perkSelectionListener;
    public boolean isGameRunning()
    {
        return !PLAYER_LIST.isEmpty();
    }
    private WaveManager waveManager;
    private MapManager mapManager;
    private BossStructureSpawner bossSpawner;
    private GameLoopManager gameLoopManager;
    public final Map<String,PerkType> ALL_PERKS = new HashMap<String,PerkType>();
    public Team EnemyTeam;
    private final PerkShopManager perkShopManager;
    public BossBar MonsterRemaining;
    public BossBar BossHealthBar;

    public void InitializeTeam()
    {
        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam("enemy");
        if(team == null)
        {
            team = Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam("enemy");
            team.color(NamedTextColor.RED);
        }
        EnemyTeam = team;

    }
    private void InitialzieBossBar()
    {
        MonsterRemaining = BossBar.bossBar(
                Component.text("Monsters Remaining: 0"),
                1.0f,
                BossBar.Color.RED,
                BossBar.Overlay.PROGRESS
        );
        BossHealthBar = BossBar.bossBar(
                Component.text("Boss Health"),
                1.0f,
                BossBar.Color.PURPLE,
                BossBar.Overlay.PROGRESS
        );
    }
    public PerkType getRandomPerk()
    {

        PerkType[] arr = ALL_PERKS.values().toArray(new PerkType[0]);
        int randomIndex = (int) (Math.random() * arr.length);
        return arr[randomIndex];
    }
    private void RegisterPerks()
    {
        //ALL_PERKS.put("Sharpshooter",new SharpshooterPerk());
        //ALL_PERKS.put("Quickdraw",new QuickdrawPerk());
        ALL_PERKS.put("brawler", new BrawlerPerk());
        ALL_PERKS.put("thickskin", new ThinkSkinPerk());
        ALL_PERKS.put("vengeance", new VengeancePerk());
        ALL_PERKS.put("scavenger", new ScavengerPerk());
        ALL_PERKS.put("arrowrain", new ArrowRain());
        ALL_PERKS.put("giantbreath", new GiantsBreath());
    }
    private void RegisterPerksListener()
    {
        for (PerkType perk : ALL_PERKS.values()) {
            plugin.getServer().getPluginManager().registerEvents(perk, plugin);
        }
    }
    private void AddExistingPlayer()
    {
        Main.world.getPlayers().forEach(player -> {
            AddPlayer(player);

        });
        for(GamePlayer p: Main.game.PLAYER_LIST.values())
        {
            p.PLAYER_TAB.playerlist_removePlaceholder();
        }
    }
    public void AddPlayer(Player player)
    {
        player.setInvulnerable(false);
        Main.tabInfo.GetTeam.get(TabInfo.TabType.PLAYERLIST_ALIVE).addEntry(player.getName());
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
        Main.tabInfo.GetTeam.get(TabInfo.TabType.PLAYERLIST_TOP).suffix(Component.text(" [" + GetPlayerCount() + "]"));

    }
    public void RemovePlayer(Player player)
    {
        PLAYER_LIST.remove(player.getUniqueId());
        PlayerNumUpdate();

    }
    public void onBossStart()
    {
        for (GamePlayer gamePlayer : PLAYER_LIST.values()) {
            gamePlayer.onBossStart();
        }
    }
    public void onBossEnd()
    {
        for (GamePlayer gamePlayer : PLAYER_LIST.values()) {
            gamePlayer.onBossEnd();
        }
    }
    public void onNextWave(int nextwavenumber)
    {
        MonsterRemaining.name(Component.text("Monsters Remaining: " + Main.game.waveManager.currentWaveEnemyCount));
        MonsterRemaining.progress(1.0f);
        for (GamePlayer gamePlayer : PLAYER_LIST.values()) {
            gamePlayer.onWaveStart(nextwavenumber);
        }
    }
    public void onEnemyKilled(Enemy enemy)
    {
        MonsterRemaining.name(Component.text("Monsters Remaining: " + ENEMY_LIST.size()));
        if(Main.game.waveManager.currentWaveEnemyCount == 0)
        {
            MonsterRemaining.progress(0f);
            return;
        }
        MonsterRemaining.progress((float) ENEMY_LIST.size() / Main.game.waveManager.currentWaveEnemyCount);
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
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                PLAYER_LIST.values().forEach(GamePlayer::Update);

            }
        };
        updateTask.runTaskTimer(plugin, 0L, 1L);
    }

    public void EndGame()
    {

        if (gameLoopManager != null) {
            gameLoopManager.handleExternalGameEnd();
        }
        Bukkit.broadcastMessage(org.bukkit.ChatColor.RED + "GAME OVER lololololo");
        PLAYER_LIST.values().forEach(GamePlayer::resetProgress);
        perkShopManager.closeShop();
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
        if (bossSpawner != null && bossSpawner.hasActiveBossRoom()) {
            bossSpawner.clearBossRoom(
                    message -> plugin.getLogger().info("Cleared boss room"),
                    error -> plugin.getLogger().info(String.format("Failed to clear boss room", error))
            );
        }
        teleportPlayersToLobby();
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

    public void setBossSpawner(BossStructureSpawner bossSpawner) {
        this.bossSpawner = bossSpawner;
    }

    public void setGameLoopManager(GameLoopManager gameLoopManager) {
        this.gameLoopManager = gameLoopManager;
    }

    public GameLoopManager getGameLoopManager() {
        return gameLoopManager;
    }

    public PerkShopManager getPerkShopManager() {
        return perkShopManager;
    }

    public void showWaveTitle(int waveNumber, int totalEnemies) {
        Title title = Title.title(
                Component.text("Wave " + waveNumber, NamedTextColor.GOLD),
                Component.text(totalEnemies + "enemies incoming", NamedTextColor.GRAY),
                WAVE_TITLE_TIMES
        );
        PLAYER_LIST.values().forEach(gamePlayer -> {
            Player bukkitPlayer = gamePlayer.getMinecraftPlayer();
            if (bukkitPlayer != null) {
                bukkitPlayer.showTitle(title);
            }
        });
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

    private void teleportPlayersToLobby() {
        if (PLAYER_LIST.isEmpty()) {
            return;
        }
        var availableWorld = Main.world != null ? Main.world : (Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0));
        if (availableWorld == null) {
            return;
        }
        Location lobby;
        if (gameLoopManager != null) {
            lobby = gameLoopManager.getConfig().resolveLobby(availableWorld);
        } else {
            lobby = new Location(availableWorld, 39, -60, 19);
        }
        if (lobby == null) {
            return;
        }
        for (GamePlayer player : PLAYER_LIST.values()) {
            Player bukkit = player.getMinecraftPlayer();
            if (bukkit != null && bukkit.isOnline()) {
                bukkit.teleport(lobby);
            }
        }
    }
}
