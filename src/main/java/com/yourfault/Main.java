package com.yourfault;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import com.yourfault.Commands.Debug.EndGame;
import com.yourfault.Commands.Debug.GetCommand;
import com.yourfault.handler.PerkSelectionHandler;
import com.yourfault.listener.PerkSelectionListener;
import com.yourfault.listener.WeaponSelectionListener;
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
    private WeaponSelectionListener weaponSelectionListener;
    private PerkSelectionListener perkSelectionListener;
    private QuickdrawAbility quickdrawAbility;
    private SharpshooterAbility sharpshooterAbility;
    @Override
    public void onEnable() {
        getLogger().info("LOT Initialized...");
        plugin = this;
        world = Bukkit.getWorld("world");
        game = new Game(this);
        var weaponSelectionHandler = new com.yourfault.handler.WeaponSelectionHandler(game.PLAYER_LIST);
        var perkSelectionHandler = new PerkSelectionHandler(game.PLAYER_LIST);
        game.setWeaponSelectionHandler(weaponSelectionHandler);
        game.setPerkSelectionHandler(perkSelectionHandler);
        weaponSelectionListener = new WeaponSelectionListener(this, weaponSelectionHandler);
        perkSelectionListener = new PerkSelectionListener(this, perkSelectionHandler);
        quickdrawAbility = new QuickdrawAbility(perkSelectionHandler);
        sharpshooterAbility = new SharpshooterAbility(this, perkSelectionHandler);
        game.setWeaponSelectionListener(weaponSelectionListener);
        game.setPerkSelectionListener(perkSelectionListener);
        RegisterCommands();
        RegisterWeapons();
    }
    private void RegisterCommands()
    {
        this.getCommand("startgame").setExecutor(new com.yourfault.Commands.Debug.StartGame());
        this.getCommand("endgame").setExecutor(new EndGame());
        this.getCommand("get").setExecutor(new GetCommand());

    }
    public void RegisterWeapons() {
        this.getServer().getPluginManager().registerEvents(new Excalibur_Main(), this);
        this.getServer().getPluginManager().registerEvents(weaponSelectionListener, this);
        this.getServer().getPluginManager().registerEvents(perkSelectionListener, this);
        this.getServer().getPluginManager().registerEvents(quickdrawAbility, this);
        this.getServer().getPluginManager().registerEvents(sharpshooterAbility, this);
    }
    @Override
    public void onDisable() {
        getLogger().info("LOT Disabled");
    }

}