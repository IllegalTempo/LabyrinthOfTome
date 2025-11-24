package com.yourfault.Commands.map;

import com.yourfault.map.MapManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ClearMapCommand implements CommandExecutor {
    private final MapManager mapManager;

    public ClearMapCommand(MapManager mapManager) {
        this.mapManager = mapManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!mapManager.hasActiveMap()) {
            sender.sendMessage(ChatColor.YELLOW + "There is no map to clear");
            return true;
        }

        int restored = mapManager.clearMap();
        sender.sendMessage(ChatColor.GREEN + "Cleared generated map and restored " + restored + " blocks.");
        return true;
    }
}
