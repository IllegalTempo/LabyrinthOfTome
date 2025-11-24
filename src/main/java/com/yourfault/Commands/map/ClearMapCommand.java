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
        if (mapManager.isGenerationRunning()) {
            sender.sendMessage(ChatColor.RED + "Map generation is still running. Wait for it to complete before clearing.");
            return true;
        }
        if (!mapManager.hasActiveMap()) {
            sender.sendMessage(ChatColor.YELLOW + "There is no map to clear");
            return true;
        }
        if (mapManager.isClearingRunning()) {
            sender.sendMessage(ChatColor.YELLOW + "Map clearing is already in progress.");
            return true;
        }
        sender.sendMessage(ChatColor.YELLOW + "Clearing generated region slice by slice...");
        mapManager.clearMapAsync(
                removed -> sender.sendMessage(ChatColor.GREEN + "Cleared generated map region and removed " + removed + " blocks."),
                error -> sender.sendMessage(ChatColor.RED + error)
        );
        return true;
    }
}
