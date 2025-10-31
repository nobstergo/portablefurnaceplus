package com.nobstergo.portablefurnaceplus.commands;

import com.nobstergo.portablefurnaceplus.item.PortableFurnaceItem;
import com.nobstergo.portablefurnaceplus.util.MessageFile;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GiveCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Permission check first
        if (!sender.hasPermission("portablefurnaceplus.admin")) {
            sender.sendMessage(MessageFile.get("no-permission"));
            return true;
        }

        // Usage check next
        if (args.length < 1) {
            sender.sendMessage(MessageFile.get("usage"));
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("give")) {
            if (args.length == 2) {
                String target = args[1];
                Player p = Bukkit.getPlayerExact(target);
                if (p == null) {
                    sender.sendMessage(MessageFile.get("player-not-online"));
                    return true;
                }
                ItemStack item = PortableFurnaceItem.createPortableFurnaceFor(p);
                p.getInventory().addItem(item);
                sender.sendMessage(MessageFile.get("item-given").replace("{player}", p.getName()));
                return true;
            } else {
                // sender gives to self
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    ItemStack item = PortableFurnaceItem.createPortableFurnaceFor(player);
                    player.getInventory().addItem(item);
                    sender.sendMessage(MessageFile.get("item-given").replace("{player}", player.getName()));
                    return true;
                } else {
                    sender.sendMessage(MessageFile.get("usage"));
                    return true;
                }
            }
        }
        sender.sendMessage(MessageFile.get("usage"));
        return true;
    }
}
