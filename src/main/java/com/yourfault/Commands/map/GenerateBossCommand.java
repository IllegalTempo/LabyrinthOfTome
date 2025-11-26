package com.yourfault.Commands.map;

import com.yourfault.map.BossStructureSpawner;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

public class GenerateBossCommand implements CommandExecutor {
    private final BossStructureSpawner spawner;

    public GenerateBossCommand(BossStructureSpawner spawner) {
        this.spawner = Objects.requireNonNull(spawner, "spawner");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only");
            return true;
        }
        if (args.length != 3) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /generateboss <x> <y> <z>");
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
        sender.sendMessage(ChatColor.YELLOW + "Starting boss room construction...");
        spawner.generateBossRoom(
                center,
                message -> sender.sendMessage(ChatColor.GREEN + message),
                error -> sender.sendMessage(ChatColor.RED + error)
        );
        return true;
    }
}
