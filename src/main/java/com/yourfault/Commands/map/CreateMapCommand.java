package com.yourfault.Commands.map;

import com.yourfault.map.MapGenerationSummary;
import com.yourfault.map.MapManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateMapCommand implements  CommandExecutor {
    private final MapManager mapManager;

    public CreateMapCommand(MapManager mapManager) {
        this.mapManager = mapManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only");
            return true;
        }

        if (args.length != 3) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /createmap <x> <y> <z>");
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

        Location center = new Location(player.getWorld(), x, y, z);
        try {
            MapGenerationSummary summary = mapManager.generateMap(center);
            sender.sendMessage(ChatColor.GREEN + "Generated a " + summary.getTheme().name().toLowerCase() + " map with radius "
                    + summary.getRadius() + " and " + summary.getSpawnMarkerCount() + " spawn markers.");
        } catch (IllegalStateException ex) {
            sender.sendMessage(ChatColor.RED + ex.getMessage());
        } catch (Exception ex) {
            sender.sendMessage(ChatColor.RED + "Failed to generate map. Check server logs.");
            ex.printStackTrace();
        }

        return true;
    }
}
