package com.yourfault.Commands.Debugs;

import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GetCoinsCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("lot.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <amount> [player]");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Amount must be a number.");
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage(ChatColor.RED + "Amount must be positive.");
            return true;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' is not online.");
                return true;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "You must specify a player when using this command from console.");
                return true;
            }
            target = player;
        }

        GamePlayer gamePlayer = Main.game.GetPlayer(target);
        if (gamePlayer == null) {
            sender.sendMessage(ChatColor.RED + "That player is not part of the active game session.");
            return true;
        }

        gamePlayer.addCoins(amount);
        target.sendMessage(ChatColor.GOLD + "You received " + amount + " coins.");
        if (!target.equals(sender)) {
            sender.sendMessage(ChatColor.GREEN + "Gave " + amount + " coins to " + target.getName() + ".");
        }
        return true;
    }
}
