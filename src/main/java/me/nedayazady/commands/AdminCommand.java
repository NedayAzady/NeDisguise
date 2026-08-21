package me.nedayazady.commands;

import me.nedayazady.NeDisguise;
import me.nedayazady.database.DisguiseData;
import me.nedayazady.listeners.PlayerListener;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminCommand implements CommandExecutor {

    private final NeDisguise plugin;

    public AdminCommand(NeDisguise plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("core.command.disguiseadmin")) {
            sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.no_permission")));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "disguise":
                if (args.length < 5) {
                    sender.sendMessage(plugin.color("&cUsage: /disguiseadmin disguise <rank> <player> <skin> <target>"));
                    return true;
                }
                
                String rank = args[1];
                String disguiseName = args[2];
                String skin = args[3];
                Player target = Bukkit.getPlayer(args[4]);
                
                if (target == null) {
                    sender.sendMessage(plugin.color("&cPlayer not found."));
                    return true;
                }
                
                DisguiseData data = new DisguiseData(target.getName(), disguiseName, rank, skin);
                plugin.getDatabaseManager().saveDisguiseData(target.getUniqueId(), data);
                
                PlayerListener playerListener = new PlayerListener(plugin);
                playerListener.changeName(target, disguiseName, rank);
                
                sender.sendMessage(plugin.color("&aDisguised " + target.getName() + " as " + disguiseName + " with rank " + rank));
                target.sendMessage(plugin.color(plugin.getConfig().getString("messages.disguised").replace("{name}", disguiseName).replace("{rank}", rank)));
                return true;

            case "undisguise":
                if (args.length < 2) {
                    sender.sendMessage(plugin.color("&cUsage: /disguiseadmin undisguise <player>"));
                    return true;
                }
                
                Player targetUndisguise = Bukkit.getPlayer(args[1]);
                if (targetUndisguise == null) {
                    sender.sendMessage(plugin.color("&cPlayer not found."));
                    return true;
                }
                
                plugin.getDatabaseManager().removeDisguiseData(targetUndisguise.getUniqueId());
                plugin.getGuiManager().sessionData.remove(targetUndisguise.getUniqueId());
                
                PlayerListener undisguiseListener = new PlayerListener(plugin);
                undisguiseListener.changeName(targetUndisguise, targetUndisguise.getName(), "");
                
                sender.sendMessage(plugin.color("&aRemoved disguise from " + targetUndisguise.getName()));
                targetUndisguise.sendMessage(plugin.color(plugin.getConfig().getString("messages.undisguised")));
                return true;

            case "listdisguised":
                sender.sendMessage(plugin.color("&e--- Disguised Players ---"));
                for (Player p : Bukkit.getOnlinePlayers()) {
                    plugin.getDatabaseManager().getDisguiseData(p.getUniqueId()).thenAccept(d -> {
                        if (d != null && d.getName() != null) {
                            sender.sendMessage(plugin.color("&a" + p.getName() + " &7is disguised as &e" + d.getName() + " &7(&b" + d.getRank() + "&7)"));
                        }
                    });
                }
                return true;

            case "checkdisguise":
                if (args.length < 2) {
                    sender.sendMessage(plugin.color("&cUsage: /disguiseadmin checkdisguise <player>"));
                    return true;
                }
                
                Player targetCheck = Bukkit.getPlayer(args[1]);
                if (targetCheck == null) {
                    sender.sendMessage(plugin.color("&cPlayer not found."));
                    return true;
                }
                
                plugin.getDatabaseManager().getDisguiseData(targetCheck.getUniqueId()).thenAccept(d -> {
                    if (d != null && d.getName() != null) {
                        sender.sendMessage(plugin.color("&a" + targetCheck.getName() + " &7is disguised as &e" + d.getName() + " &7(&b" + d.getRank() + "&7)"));
                    } else {
                        sender.sendMessage(plugin.color("&c" + targetCheck.getName() + " is not disguised."));
                    }
                });
                return true;

            case "history":
                sender.sendMessage(plugin.color("&cHistory feature is not implemented yet."));
                return true;

            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.color("&cAdmin Commands:"));
        sender.sendMessage(plugin.color("&7/disguiseadmin disguise <rank> <player> <skin> <target>"));
        sender.sendMessage(plugin.color("&7/disguiseadmin undisguise <player>"));
        sender.sendMessage(plugin.color("&7/disguiseadmin listdisguised"));
        sender.sendMessage(plugin.color("&7/disguiseadmin checkdisguise <player>"));
        sender.sendMessage(plugin.color("&7/disguiseadmin history <player>"));
    }
}