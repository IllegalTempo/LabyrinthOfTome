package com.yourfault;

import com.yourfault.Commands.Debugs.*;
import com.yourfault.Commands.map.ClearMapCommand;
import com.yourfault.Commands.map.CreateMapCommand;
import com.yourfault.map.MapManager;
import com.yourfault.npcinteraction.WeaponSelect;
import com.yourfault.system.BleedoutManager;
import com.yourfault.system.CustomHealthListener;
import com.yourfault.system.PlayerActivity;
import com.yourfault.Enemy.EnemyHealthDisplay;
import com.yourfault.wave.WaveCombatListener;
import com.yourfault.wave.WaveManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import com.yourfault.listener.PerkSelectionListener;
import com.yourfault.perks.quickdraw.QuickdrawAbility;
import com.yourfault.perks.sharpshooter.SharpshooterAbility;
import com.yourfault.system.Game;
import com.yourfault.weapon.Excalibur.Excalibur_Main;


//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main extends JavaPlugin {
    public static JavaPlugin plugin;
    public static Game game;
    public static World world;
    public static BleedoutManager bleedoutManager;
    public static EnemyHealthDisplay enemyHealthDisplay;
    private PerkSelectionListener perkSelectionListener;
    private WaveManager waveManager;
    private MapManager mapManager;
    private BleedoutManager bleedManager;
    private EnemyHealthDisplay healthDisplay;

    //private QuickdrawAbility quickdrawAbility;
    //private SharpshooterAbility sharpshooterAbility;

    @Override
    public void onEnable() {
        getLogger().info("LOT Initialized...");
        plugin = this;
        world = Bukkit.getWorld("world");
        game = new Game(this);
        mapManager = new MapManager(this, game);
        bleedManager = new BleedoutManager(this, game);
        bleedoutManager = bleedManager;
        this.getServer().getPluginManager().registerEvents(bleedManager, this);
        healthDisplay = new EnemyHealthDisplay(this);
        enemyHealthDisplay = healthDisplay;
        this.getServer().getPluginManager().registerEvents(healthDisplay, this);
        waveManager = new WaveManager(game, mapManager, healthDisplay);
        game.setWaveManager(waveManager);

        perkSelectionListener = new PerkSelectionListener(this);

        game.setPerkSelectionListener(perkSelectionListener);
        RegisterCommands();
        RegisterWeapons();
        RegisterAbilityListeners();
        RegisterNPCInteraction();
        RegisterPlayerActivity();
        RegisterCustomHealthSystem();
    }
    private void RegisterCommands()
    {
        this.getCommand("startgame").setExecutor(new StartGame());
        this.getCommand("endgame").setExecutor(new EndGameCommand());
        this.getCommand("get").setExecutor(new GetCommand());
        this.getCommand("readyfornextwave").setExecutor(new ReadyForNextWave());
        this.getCommand("spawnnpc").setExecutor(new SummonNPC());
        this.getCommand("getcoins").setExecutor(new GetCoinsCommand());
        this.getCommand("createmap").setExecutor(new CreateMapCommand(mapManager));
        this.getCommand("clearmap").setExecutor(new ClearMapCommand(mapManager));
        this.getCommand("skipwave").setExecutor(new SkipWaveCommand(game));
        this.getCommand("damageself").setExecutor(new changehealth());

    }
    private void RegisterPlayerActivity()
    {
        this.getServer().getPluginManager().registerEvents(new PlayerActivity(),this);
    }
    private void RegisterNPCInteraction()
    {
        this.getServer().getPluginManager().registerEvents(new WeaponSelect(),this);
    }
    private void RegisterAbilityListeners() {
        this.getServer().getPluginManager().registerEvents(new QuickdrawAbility(), this);
        this.getServer().getPluginManager().registerEvents(new SharpshooterAbility(), this);
        this.getServer().getPluginManager().registerEvents(new WaveCombatListener(game), this);


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
    }

}