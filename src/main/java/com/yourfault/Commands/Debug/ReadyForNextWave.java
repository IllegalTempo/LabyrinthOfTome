package com.yourfault.Commands.Debug;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.yourfault.Main;

public class ReadyForNextWave implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (Main.game == null || Main.game.getWaveManager() == null) {
            sender.sendMessage(ChatColor.RED + "Wave manager not initialized.");
            return true;
        }
        Main.game.getWaveManager().triggerNextWave();
        return true;
    }
}
