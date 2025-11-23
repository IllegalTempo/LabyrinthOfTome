package com.yourfault.Commands.Debugs;

import com.yourfault.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class EndGameCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (Main.game != null) {
            Main.game.EndGame();
            Bukkit.broadcastMessage("§cGame ended by " + sender.getName());
        }
        return true;
    }
}
