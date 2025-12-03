package com.yourfault.Commands.gameloop;

import com.yourfault.gameloop.GameLoopManager;
import com.yourfault.gameloop.ReadyAction;
import com.yourfault.system.Game;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReadyCommand implements CommandExecutor {
    private final Game game;

    public ReadyCommand(Game game) {
        this.game = game;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof  Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        GameLoopManager manager = game.getGameLoopManager();
        if (manager == null) {
            player.sendMessage(ChatColor.RED + "Game loop manager is not available.");
            return true;
        }
        ReadyAction action = parseAction(args);
        if (action == null) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /" + label + "[yes|no]");
            return true;
        }
        manager.handleReadyCommand(player, action);
        return true;
    }

    private ReadyAction parseAction(String[] args) {
        if (args.length == 0) {
            return ReadyAction.CONFIRM;
        }
        String token = args[0].toLowerCase();
        if (token.equals("yes") || token.equals("y")) {
            return ReadyAction.YES;
        }
        if (token.equals("no") || token.equals("n")) {
            return ReadyAction.NO;
        }
        return null;

    }
}
