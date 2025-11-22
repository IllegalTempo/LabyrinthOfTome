package com.yourfault;

import com.yourfault.Commands.Debug.StartGame;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import com.yourfault.Commands.Debug.EndGame;
import com.yourfault.Commands.Debug.GetCommand;
import com.yourfault.listener.PerkSelectionListener;
import com.yourfault.perk.quickdraw.QuickdrawAbility;
import com.yourfault.perk.sharpshooter.SharpshooterAbility;
import com.yourfault.system.Game;
import com.yourfault.weapon.Excalibur.Excalibur_Main;


//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main extends JavaPlugin {
    public static JavaPlugin plugin;
    public static Game game;
    public static World world;
    private PerkSelectionListener perkSelectionListener;

    //private QuickdrawAbility quickdrawAbility;
    //private SharpshooterAbility sharpshooterAbility;

    @Override
    public void onEnable() {
        getLogger().info("LOT Initialized...");
        plugin = this;
        world = Bukkit.getWorld("world");
        game = new Game(this);

        perkSelectionListener = new PerkSelectionListener(this);

        game.setPerkSelectionListener(perkSelectionListener);
        RegisterCommands();
        RegisterWeapons();
        RegisterAbilityListeners();
    }
    private void RegisterCommands()
    {
        this.getCommand("startgame").setExecutor(new StartGame());
        this.getCommand("endgame").setExecutor(new EndGame());
        this.getCommand("get").setExecutor(new GetCommand());

    }
    private void RegisterAbilityListeners() {
        this.getServer().getPluginManager().registerEvents(new QuickdrawAbility(), this);
        this.getServer().getPluginManager().registerEvents(new SharpshooterAbility(), this);


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