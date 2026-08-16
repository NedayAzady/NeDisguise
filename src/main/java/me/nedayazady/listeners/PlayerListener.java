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
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

public class PlayerListener implements Listener {

    private final NeDisguise plugin;

    public PlayerListener(NeDisguise plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Load data on join
        plugin.getDatabaseManager().getDisguiseData(player.getUniqueId()).thenAccept(data -> {
            if (data != null && data.getName() != null) {
                // Apply disguise logic here
                Bukkit.getScheduler().runTask(plugin, () -> {
                    changeName(player, data.getName());
                });
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getGuiManager().awaitingChatInput.remove(event.getPlayer().getUniqueId());
        plugin.getGuiManager().sessionData.remove(event.getPlayer().getUniqueId());
    }
    
    private void changeName(Player player, String newName) {
        try {
            Method getHandle = player.getClass().getMethod("getHandle");
            Object entityPlayer = getHandle.invoke(player);
            
            Object gameProfile = entityPlayer.getClass().getMethod("getProfile").invoke(entityPlayer);
            Field nameField = gameProfile.getClass().getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(gameProfile, newName);
            
            player.setDisplayName(newName);
            player.setPlayerListName(newName);
            player.setCustomName(newName);
            player.setCustomNameVisible(true);
            
            // Reload the player for others
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p != player) {
                    p.hidePlayer(player);
                    p.showPlayer(player);
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
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
