package com.yourfault;

import com.yourfault.system.Game;
import org.bukkit.plugin.java.JavaPlugin;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main extends JavaPlugin {
    public static JavaPlugin plugin;
    public static Game game;
    @Override
    public void onEnable() {
        getLogger().info("LOT Initialized...");
        plugin = this;
        game = new Game(this);
        RegisterCommands();
    }
    private void RegisterCommands()
    {
        this.getCommand("startgame").setExecutor(new com.yourfault.Commands.Debug.StartGame());

    }

    @Override
    public void onDisable() {
        getLogger().info("LOT Disabled");
    }

}