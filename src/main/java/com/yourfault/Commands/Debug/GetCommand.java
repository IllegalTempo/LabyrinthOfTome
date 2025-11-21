package com.yourfault.Commands.Debug;

import com.yourfault.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.yourfault.Items.weapons;

public class GetCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("This command can only be used by players.");
            return true;
        }
        Player player = (Player) commandSender;
        if (strings.length == 0) {
            player.sendMessage("Please specify an item. Available: " + String.join(", ", weapons.ITEM_MAP.keySet()));
            return true;
        }
        String itemName = strings[0].toLowerCase();
        ItemStack item = weapons.ITEM_MAP.get(itemName);
        if (item != null) {
            player.getInventory().addItem(item.clone());
            player.sendMessage("You have been given " + itemName + "!");
            return true;
        }
        player.sendMessage("Unknown item. Available: " + String.join(", ", weapons.ITEM_MAP.keySet()));
        return true;
    }
}
