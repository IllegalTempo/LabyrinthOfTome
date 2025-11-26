package com.yourfault.Commands.map;

import com.yourfault.map.BossStructureSpawner;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Objects;

public class ClearBossRoomCommand implements CommandExecutor {
    private final BossStructureSpawner spawner;

    public ClearBossRoomCommand(BossStructureSpawner spawner) {
        this.spawner = Objects.requireNonNull(spawner, "spawner");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        spawner.clearBossRoom(
                message -> sender.sendMessage(ChatColor.GREEN + message),
                error -> sender.sendMessage(ChatColor.RED + error)
        );
        return true;
    }
}
