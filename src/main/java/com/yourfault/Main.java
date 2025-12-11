package com.yourfault;

import com.yourfault.Commands.Debugs.*;
import com.yourfault.Commands.gameloop.PlayGameCommand;
import com.yourfault.Commands.gameloop.ReadyCommand;
import com.yourfault.Commands.map.ClearBossRoomCommand;
import com.yourfault.Commands.map.ClearMapCommand;
import com.yourfault.Commands.map.CreateMapCommand;
import com.yourfault.Commands.map.GenerateBossCommand;
import com.yourfault.listener.PerkSelectionListener;
import com.yourfault.map.BossStructureSpawner;
import com.yourfault.map.MapManager;
import com.yourfault.gameloop.GameLoopManager;
import com.yourfault.npcinteraction.WeaponSelect;
import com.yourfault.system.*;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveCombatListener;
import com.yourfault.wave.WaveManager;
import com.yourfault.weapon.Excalibur.Excalibur_Main;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;


//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main extends JavaPlugin {
    public static JavaPlugin plugin;
    public static Game game;
    public static World world;
    public static BleedoutManager bleedoutManager;
    public static TabInfo tabInfo;



    private PerkSelectionListener perkSelectionListener;
    private WaveManager waveManager;
    private MapManager mapManager;
    private BossStructureSpawner bossStructureSpawner;
    private GameLoopManager gameLoopManager;
    private BleedoutManager bleedManager;

    //private QuickdrawAbility quickdrawAbility;
    //private SharpshooterAbility sharpshooterAbility;

    @Override
    public void onEnable() {

        getLogger().info("LOT Initialized...");
        plugin = this;
        world = Bukkit.getWorld("world");
        tabInfo = new TabInfo();

        new Game(this);
        mapManager = new MapManager(this, game);
        bossStructureSpawner = new BossStructureSpawner(this);
        bleedManager = new BleedoutManager(this, game);
        bleedoutManager = bleedManager;
        this.getServer().getPluginManager().registerEvents(bleedManager, this);
        waveManager = new WaveManager(game, mapManager);
        game.setWaveManager(waveManager);
        game.setBossSpawner(bossStructureSpawner);
        gameLoopManager = new GameLoopManager(this, game, mapManager, bossStructureSpawner, waveManager);
        game.setGameLoopManager(gameLoopManager);

        perkSelectionListener = new PerkSelectionListener(this);

        game.setPerkSelectionListener(perkSelectionListener);
        RegisterCommands();
        RegisterWeapons();
        RegisterNPCInteraction();
        RegisterPlayerActivity();
        RegisterCustomHealthSystem();
    }
    private void RegisterCommands()
    {
        this.getCommand("startgame").setExecutor(new StartGame());
        this.getCommand("endgame").setExecutor(new EndGameCommand());
        this.getCommand("get").setExecutor(new GetCommand());
        this.getCommand("giveperk").setExecutor((new GivePerkCommand()));
        this.getCommand("readyfornextwave").setExecutor(new ReadyForNextWave());
        this.getCommand("spawnnpc").setExecutor(new SummonNPC());
        this.getCommand("getcoins").setExecutor(new GetCoinsCommand());
        this.getCommand("createmap").setExecutor(new CreateMapCommand(mapManager));
        this.getCommand("clearmap").setExecutor(new ClearMapCommand(mapManager));
        this.getCommand("generateboss").setExecutor(new GenerateBossCommand(bossStructureSpawner));
        this.getCommand("clearbossroom").setExecutor(new ClearBossRoomCommand(bossStructureSpawner));
        this.getCommand("skipwave").setExecutor(new SkipWaveCommand(game));
        this.getCommand("damageself").setExecutor(new changehealth());
        this.getCommand("debugtab").setExecutor(new SetHeaderAndFooter());
        this.getCommand("spawnmob").setExecutor(new SpawnMobCommand(game));
        this.getCommand("playgame").setExecutor(new PlayGameCommand(game));
        this.getCommand("ready").setExecutor(new ReadyCommand(game));

    }
    private void RegisterPlayerActivity()
    {
        this.getServer().getPluginManager().registerEvents(new PlayerActivity(),this);
        this.getServer().getPluginManager().registerEvents(new WaveCombatListener(game), this);
    }
    private void RegisterNPCInteraction()
    {
        this.getServer().getPluginManager().registerEvents(new WeaponSelect(),this);
    }

    private void RegisterCustomHealthSystem() {
        this.getServer().getPluginManager().registerEvents(new CustomHealthListener(game), this);
    }
    public void RegisterWeapons() {
        this.getServer().getPluginManager().registerEvents(new Excalibur_Main(), this);
        this.getServer().getPluginManager().registerEvents(perkSelectionListener, this);
    }
    @Override
    public void onDisable() {

        getLogger().info("LOT Disabled");
        //remove all fakeplayers
        for (GamePlayer player : game.PLAYER_LIST.values()) {
            player.PLAYER_TAB.removeAllFakePlayer();
        }

    }

}