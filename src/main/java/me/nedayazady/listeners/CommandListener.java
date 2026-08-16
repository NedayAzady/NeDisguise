package me.nedayazady.listeners;

import me.nedayazady.NeDisguise;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

public class CommandListener implements Listener {

    private final NeDisguise plugin;

    public CommandListener(NeDisguise plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().toLowerCase();
        
        // Remove the slash for matching
        String command = message.split(" ")[0].substring(1);

        List<String> aliases = plugin.getConfig().getStringList("commands");
        for (String alias : aliases) {
            String checkAlias = alias.startsWith("/") ? alias.substring(1) : alias;
            
            if (command.equalsIgnoreCase(checkAlias)) {
                event.setCancelled(true);
                
                String permission = plugin.getConfig().getString("permission", "disguise.perm");
                if (!player.hasPermission(permission)) {
                    player.sendMessage(plugin.color(plugin.getConfig().getString("messages.no_permission")));
                    return;
                }
                
                plugin.getGuiManager().openRankSelectionGui(player);
                break;
            }
        }
    }
}
