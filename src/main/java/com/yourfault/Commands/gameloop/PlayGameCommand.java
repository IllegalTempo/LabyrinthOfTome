package com.yourfault.Commands.gameloop;

import com.yourfault.gameloop.GameLoopManager;
import com.yourfault.system.Game;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlayGameCommand implements CommandExecutor {
    private final Game game;

    public PlayGameCommand(Game game) {
        this.game = game;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only");
            return true;
        }
        GameLoopManager manager = game.getGameLoopManager();
        if (manager == null) {
            player.sendMessage(ChatColor.RED + "Loop system is not available");
            return true;
        }
        manager.startPlayCommand(player);
        return true;
    }
}
