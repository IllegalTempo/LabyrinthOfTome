package com.yourfault;

import com.yourfault.Commands.Debug.GetCommand;
import com.yourfault.listener.WeaponSelectionListener;
import com.yourfault.system.Game;
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
    private WeaponSelectionListener weaponSelectionListener;
    @Override
    public void onEnable() {
        getLogger().info("LOT Initialized...");
        plugin = this;
        world = Bukkit.getWorld("world");
        game = new Game(this);
        var weaponSelectionHandler = new com.yourfault.handler.WeaponSelectionHandler(game.PLAYER_LIST);
        game.setWeaponSelectionHandler(weaponSelectionHandler);
        weaponSelectionListener = new WeaponSelectionListener(this, game, weaponSelectionHandler);
        game.setWeaponSelectionListener(weaponSelectionListener);
        RegisterCommands();
        RegisterWeapons();
    }
    private void RegisterCommands()
    {
        this.getCommand("startgame").setExecutor(new com.yourfault.Commands.Debug.StartGame());
        this.getCommand("get").setExecutor(new GetCommand());

    }
    public void RegisterWeapons() {
        this.getServer().getPluginManager().registerEvents(new Excalibur_Main(), this);
        this.getServer().getPluginManager().registerEvents(weaponSelectionListener, this);
    }
    @Override
    public void onDisable() {
        getLogger().info("LOT Disabled");
    }

}