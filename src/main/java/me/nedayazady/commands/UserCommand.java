package me.nedayazady.commands;

import me.nedayazady.NeDisguise;
import me.nedayazady.database.DisguiseData;
import me.nedayazady.listeners.PlayerListener;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UserCommand implements CommandExecutor {

    private final NeDisguise plugin;

    public UserCommand(NeDisguise plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("core.command.user.disguise")) {
            sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.no_permission")));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.color("&cUsage: /user disguise <player> [rank]"));
            return true;
        }

        if (args[0].equalsIgnoreCase("disguise")) {
            Player target = Bukkit.getPlayer(args[1]);
            
            if (target == null) {
                sender.sendMessage(plugin.color("&cPlayer not found."));
                return true;
            }
            
            if (args.length == 2) {
                // No rank provided, undisguise
                plugin.getDatabaseManager().removeDisguiseData(target.getUniqueId());
                plugin.getGuiManager().sessionData.remove(target.getUniqueId());
                
                PlayerListener playerListener = new PlayerListener(plugin);
                playerListener.changeName(target, target.getName(), "");
                
                sender.sendMessage(plugin.color("&aUndisguised " + target.getName()));
                target.sendMessage(plugin.color(plugin.getConfig().getString("messages.undisguised")));
            } else {
                // Rank provided, rank disguise
                String rank = args[2];
                // Generate a generic name based on rank or use their own name but just change rank tag? 
                // Using their own name but changing rank representation in TAB
                String disguiseName = target.getName();
                
                DisguiseData data = new DisguiseData(target.getName(), disguiseName, rank, "texture_placeholder");
                plugin.getDatabaseManager().saveDisguiseData(target.getUniqueId(), data);
                
                PlayerListener playerListener = new PlayerListener(plugin);
                playerListener.changeName(target, disguiseName, rank);
                
                sender.sendMessage(plugin.color("&aRank-disguised " + target.getName() + " with rank " + rank));
                target.sendMessage(plugin.color(plugin.getConfig().getString("messages.disguised").replace("{name}", disguiseName).replace("{rank}", rank)));
            }
            return true;
        }

        sender.sendMessage(plugin.color("&cUsage: /user disguise <player> [rank]"));
        return true;
    }
}