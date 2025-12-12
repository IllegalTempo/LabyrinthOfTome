package com.yourfault.Commands.Debugs;

import com.yourfault.Main;
import com.yourfault.perks.PerkType;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public class GivePerkCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /giveperk <player> <perkName> [level]");
            return true;
        }

        String targetName = args[0];
        Player bukkitTarget = Bukkit.getPlayerExact(targetName);
        if (bukkitTarget == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + targetName);
            return true;
        }

        String perkName = args[1];
        PerkType perk = null;
        // try direct lookup first
        if (Main.game != null && Main.game.ALL_PERKS != null) {
            perk = Main.game.ALL_PERKS.get(perkName);
            if (perk == null) {
                // case-insensitive search
                for (String key : Main.game.ALL_PERKS.keySet()) {
                    if (key.equalsIgnoreCase(perkName)) {
                        perk = Main.game.ALL_PERKS.get(key);
                        break;
                    }
                }
            }
        }

        if (perk == null) {
            sender.sendMessage(ChatColor.RED + "Perk not found: " + perkName);
            return true;
        }

        GamePlayer gp = Main.game.GetPlayer(bukkitTarget);
        if (gp == null) {
            sender.sendMessage(ChatColor.RED + "Target player is not in a run: " + targetName);
            return true;
        }

        // Add perk
        boolean added = gp.PLAYER_PERKS.addPerk(perk);
        if (!added) {
            sender.sendMessage(ChatColor.YELLOW + "Perk already present or failed to add: " + perk.displayName);
        } else {
            // Optional level parameter
            int level = 1;
            if (args.length >= 3) {
                try {
                    level = Math.max(1, Integer.parseInt(args[2]));
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.YELLOW + "Invalid level; defaulting to 1.");
                    level = 1;
                }
            }

            gp.PLAYER_PERKS.preparePerkSlots();
            // level up to desired level
            for (int i = 1; i < level; i++) {
                gp.PLAYER_PERKS.levelUpPerk(perk);
            }

            // Recalculate stats on player to apply perk effects
            //gp.recalculateStats();

            sender.sendMessage(ChatColor.GREEN + "Gave perk " + perk.displayName + " (Level " + gp.PLAYER_PERKS.getPerkLevel(perk) + ") to " + bukkitTarget.getName());
            bukkitTarget.sendMessage(ChatColor.GREEN + "You received perk: " + perk.displayName + " (Level " + gp.PLAYER_PERKS.getPerkLevel(perk) + ")");
        }

        return true;
    }
}
