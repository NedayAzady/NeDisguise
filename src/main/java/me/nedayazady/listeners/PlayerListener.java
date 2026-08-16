package me.nedayazady.listeners;

import me.nedayazady.NeDisguise;
import me.nedayazady.database.DisguiseData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final NeDisguise plugin;

    public PlayerListener(NeDisguise plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Load data on join
        plugin.getDatabaseManager().getDisguiseData(event.getPlayer().getUniqueId()).thenAccept(data -> {
            if (data != null) {
                // Apply disguise logic here (e.g. modify GameProfile, NickAPI, etc)
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getGuiManager().awaitingChatInput.remove(event.getPlayer().getUniqueId());
        plugin.getGuiManager().sessionData.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.getGuiManager().awaitingChatInput.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            String message = event.getMessage().trim();

            if (message.equalsIgnoreCase("cancel")) {
                plugin.getGuiManager().awaitingChatInput.remove(player.getUniqueId());
                player.sendMessage(plugin.color(plugin.getConfig().getString("messages.cancel_disguise")));
                return;
            }

            if (message.length() > 16) {
                player.sendMessage(plugin.color(plugin.getConfig().getString("messages.name_too_long")));
                return;
            }
            
            if (!message.matches("^[a-zA-Z0-9_]+$")) {
                player.sendMessage(plugin.color(plugin.getConfig().getString("messages.invalid_name")));
                return;
            }

            plugin.getGuiManager().awaitingChatInput.remove(player.getUniqueId());
            DisguiseData data = plugin.getGuiManager().sessionData.get(player.getUniqueId());
            if (data != null) {
                data.setName(message);
                // Return to main thread to open GUI
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openConfirmGui(player));
            }
        }
    }
}
