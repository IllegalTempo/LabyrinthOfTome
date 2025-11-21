package com.yourfault;

import org.bukkit.plugin.java.JavaPlugin;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main extends JavaPlugin {
        @Override
        public void onEnable() {
            getLogger().info("LOT Initialized...");
        }

        @Override
        public void onDisable() {
            getLogger().info("LOT Disabled");
        }


}