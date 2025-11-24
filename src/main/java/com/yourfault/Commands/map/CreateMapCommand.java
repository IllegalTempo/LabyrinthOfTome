package com.yourfault.Commands.map;

import com.yourfault.map.MapManager;
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

public class CreateMapCommand implements  CommandExecutor {
    private final MapManager mapManager;

    public CreateMapCommand(MapManager mapManager) {
        this.mapManager = mapManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Objects.requireNonNull(sender, "sender");
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only");
            return true;
        }

        if (args.length < 3 || args.length > 4) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /createmap <x> <y> <z> [theme]");
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
        if (args.length == 4) {
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
        sender.sendMessage(ChatColor.YELLOW + "Starting slice-based map generation..."
            + (requestedTheme != null ? " Theme: " + requestedTheme.name().toLowerCase() : ""));
        MapTheme finalRequestedTheme = requestedTheme;
        mapManager.generateMapAsync(center,
            finalRequestedTheme,
            summary -> sender.sendMessage(ChatColor.GREEN + "Generated a "
                + summary.getTheme().name().toLowerCase() + " map with radius "
                + summary.getRadius() + " and " + summary.getSpawnMarkerCount() + " spawn markers."),
            error -> sender.sendMessage(ChatColor.RED + error));

        return true;
    }
}
