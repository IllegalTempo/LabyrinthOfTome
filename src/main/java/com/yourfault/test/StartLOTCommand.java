package com.yourfault.test;

import com.yourfault.map.MapTheme;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class StartLOTCommand implements CommandExecutor {
    private final LOTTestScenario scenario;

    public StartLOTCommand(LOTTestScenario scenario) {
        this.scenario = Objects.requireNonNull(scenario, "scenario");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /startLOT <x> <y> <z> [theme]");
            return true;
        }
        int x;
        int y;
        int z;
        try {
            x = Integer.parseInt(args[0]);
            y = Integer.parseInt(args[1]);
            z = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Coordinates must be integers.");
            return true;
        }
        MapTheme requestedTheme = null;
        if (args.length >= 4) {
            requestedTheme = MapTheme.findByName(args[3]);
            if (requestedTheme == null) {
                String options = Arrays.stream(MapTheme.values())
                        .map(theme -> theme.name().toLowerCase())
                        .collect(Collectors.joining(ChatColor.GRAY + ", " + ChatColor.YELLOW));
                sender.sendMessage(ChatColor.RED + "Unknown theme '" + args[3] + "'.");
                sender.sendMessage(ChatColor.YELLOW + "Available: " + ChatColor.GRAY + options);
                return true;
            }
        }
        Location center = new Location(player.getWorld(), x, y, z);
        scenario.startScenario(player, center, requestedTheme, sender);
        return true;
    }
}
