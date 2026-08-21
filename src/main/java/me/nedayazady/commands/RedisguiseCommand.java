package me.nedayazady.commands;

import me.nedayazady.NeDisguise;
import me.nedayazady.database.DisguiseData;
import me.nedayazady.listeners.PlayerListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Random;

public class RedisguiseCommand implements CommandExecutor {

    private final NeDisguise plugin;
    private final Random random = new Random();
    private final String[] prefixes = {"Pro", "Noob", "xX", "The", "Epic", "Dark", "Ghost", "Ninja", "Super", "Mega", "Ultra", "Fast", "Iron", "Gold", "Shadow", "King", "Wolf"};
    private final String[] suffixes = {"Gamer", "PVP", "Slayer", "Craft", "Boy", "Girl", "HD", "YT", "MC", "King", "Beast", "Master", "Lord", "Knight", "Hero", "Mine"};

    public RedisguiseCommand(NeDisguise plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("core.command.redisguise")) {
            player.sendMessage(plugin.color(plugin.getConfig().getString("messages.no_permission")));
            return true;
        }

        plugin.getDatabaseManager().getDisguiseData(player.getUniqueId()).thenAccept(data -> {
            if (data != null && data.getName() != null && data.getRank() != null) {
                // Reapply disguise
                PlayerListener listener = new PlayerListener(plugin);
                listener.changeName(player, data.getName(), data.getRank());
                player.sendMessage(plugin.color(plugin.getConfig().getString("messages.redisguised")
                        .replace("{name}", data.getName())
                        .replace("{rank}", data.getRank())));
            } else {
                // If never disguised, generate random and apply
                String prefix = prefixes[random.nextInt(prefixes.length)];
                String suffix = suffixes[random.nextInt(suffixes.length)];
                String randName = prefix + suffix + random.nextInt(99);
                String randRank = "default";
                String randSkin = "Steve";

                DisguiseData newData = new DisguiseData(player.getName(), randName, randRank, randSkin);
                plugin.getDatabaseManager().saveDisguiseData(player.getUniqueId(), newData);

                PlayerListener listener = new PlayerListener(plugin);
                listener.changeName(player, randName, randRank);

                player.sendMessage(plugin.color(plugin.getConfig().getString("messages.redisguised")
                        .replace("{name}", randName)
                        .replace("{rank}", randRank)));
            }
        });

        return true;
    }
}
