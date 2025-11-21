package com.yourfault;

import com.yourfault.Commands.Debug.GetCommand;
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
    public static World world = Bukkit.getWorld("world");
    @Override
    public void onEnable() {
        getLogger().info("LOT Initialized...");
        plugin = this;
        game = new Game(this);
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
    }
    @Override
    public void onDisable() {
        getLogger().info("LOT Disabled");
    }

}