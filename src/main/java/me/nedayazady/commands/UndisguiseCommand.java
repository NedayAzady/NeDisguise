package me.nedayazady.commands;

import me.nedayazady.NeDisguise;
import me.nedayazady.listeners.PlayerListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UndisguiseCommand implements CommandExecutor {

    private final NeDisguise plugin;

    public UndisguiseCommand(NeDisguise plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        
        if (!player.hasPermission("core.command.undisguise")) {
            player.sendMessage(plugin.color(plugin.getConfig().getString("messages.no_permission")));
            return true;
        }

        plugin.getDatabaseManager().removeDisguiseData(player.getUniqueId());
        plugin.getGuiManager().sessionData.remove(player.getUniqueId());
        
        PlayerListener playerListener = new PlayerListener(plugin);
        playerListener.changeName(player, player.getName(), "");
        
        player.sendMessage(plugin.color(plugin.getConfig().getString("messages.undisguised")));
        return true;
    }
}