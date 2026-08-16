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
            
            // Reload the player for others to update tab and nametags properly
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p != player) {
                    p.hidePlayer(player);
                    // Add a slight delay before showing to ensure client clears cache
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        p.showPlayer(player);
                    }, 2L);
                }
            }
            
            // Force self update if possible
            player.hidePlayer(player);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.showPlayer(player);
            }, 2L);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // The chat listener for name input is no longer needed as we use SignGUI now
    // @EventHandler
    // public void onChat(AsyncPlayerChatEvent event) { ... }
}
