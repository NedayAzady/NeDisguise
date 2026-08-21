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
import java.util.List;

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
        // Delay fetching and applying disguise to allow TAB/NTE to initialize their scoreboards first
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getDatabaseManager().getDisguiseData(player.getUniqueId()).thenAccept(data -> {
                if (data != null && data.getName() != null && data.getRank() != null) {
                    // Update their real name in memory if it was missing (from older versions)
                    if (data.getRealName() == null) {
                        data.setRealName(player.getName());
                    }
                    
                    // Apply disguise logic here on main thread
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        changeName(player, data.getName(), data.getRank());
                    });
                }
            });
        }, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getGuiManager().sessionData.remove(event.getPlayer().getUniqueId());
    }
    
    public void changeName(Player player, String newName, String rank) {
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
            
            // Execute configured commands to update TAB/Tags plugins
            List<String> commands;
            boolean isUndisguise = rank == null || rank.isEmpty();
            if (isUndisguise) {
                commands = plugin.getConfig().getStringList("on_undisguise_commands");
            } else {
                commands = plugin.getConfig().getStringList("on_disguise_commands");
            }
            
            for (String cmd : commands) {
                // Determine original name safely
                String originalName = player.getName(); 
                try {
                    DisguiseData dbData = plugin.getDatabaseManager().getDisguiseData(player.getUniqueId()).get();
                    if (dbData != null && dbData.getRealName() != null) {
                        originalName = dbData.getRealName();
                    }
                } catch (Exception ignored) {}
                
                String formattedCmd = cmd.replace("{player}", originalName);
                if (!isUndisguise) {
                     formattedCmd = formattedCmd.replace("{name}", newName);
                }
                
                // If it's a tab command to remove the properties, we need to send the command 
                // formatted correctly for Nezamy TAB, which means NO value argument if it's clear.
                // Let's ensure no trailing spaces that could cause it to run twice incorrectly.
                formattedCmd = formattedCmd.trim();
                
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), formattedCmd);
            }
            
            // Reload the player for others to update tab and nametags properly
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p != player) {
                    p.hidePlayer(player);
                    // Extremely short delay to force client update instantly
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        p.showPlayer(player);
                    }, 1L);
                }
            }
            
            // Force self update if possible
            player.hidePlayer(player);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.showPlayer(player);
            }, 1L);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // The chat listener for name input is no longer needed as we use SignGUI now
    // @EventHandler
    // public void onChat(AsyncPlayerChatEvent event) { ... }
}
