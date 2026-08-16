package me.nedayazady.listeners;

import me.nedayazady.NeDisguise;
import me.nedayazady.database.DisguiseData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class GuiListener implements Listener {

    private final NeDisguise plugin;

    public GuiListener(NeDisguise plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (title == null) return;

        String rankTitle = ChatColor.stripColor(plugin.color(plugin.getConfig().getString("gui.ranks.title")));
        String skinTitle = ChatColor.stripColor(plugin.color(plugin.getConfig().getString("gui.skins.title")));
        String confirmTitle = ChatColor.stripColor(plugin.color(plugin.getConfig().getString("gui.confirm.title")));

        boolean isDisguiseGui = title.equals(rankTitle) || title.equals(skinTitle) || title.equals(confirmTitle);
        if (!isDisguiseGui) return;

        event.setCancelled(true);

        if (event.getRawSlot() >= event.getInventory().getSize() || event.getRawSlot() < 0) {
            return; // Clicked outside or in player inventory
        }

        ItemStack currentItem = event.getCurrentItem();
        if (currentItem == null || currentItem.getType() == Material.AIR) return;

        if (title.equals(rankTitle)) {
            handleRankSelection(player, currentItem);
        } else if (title.equals(skinTitle)) {
            handleSkinSelection(player, currentItem);
        } else if (title.equals(confirmTitle)) {
            handleConfirmation(player, currentItem);
        }
    }

    private void playSoundSafe(Player player, String soundName) {
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (IllegalArgumentException e) {
            // Sound doesn't exist in this version, ignore or log a warning
            plugin.getLogger().warning("Invalid sound name in config: " + soundName);
        }
    }

    private void handleRankSelection(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        String displayName = ChatColor.stripColor(meta.getDisplayName());
        
        Material nextMat = Material.valueOf(plugin.getConfig().getString("gui.ranks.next_button.material"));
        Material cancelMat = Material.valueOf(plugin.getConfig().getString("gui.ranks.cancel_button.material"));
        
        String nextName = ChatColor.stripColor(plugin.color(plugin.getConfig().getString("gui.ranks.next_button.name")));
        String cancelName = ChatColor.stripColor(plugin.color(plugin.getConfig().getString("gui.ranks.cancel_button.name")));

        if (item.getType() == cancelMat && displayName.contains(cancelName)) {
            player.closeInventory();
            playSoundSafe(player, plugin.getConfig().getString("sounds.cancel", "NOTE_BASS"));
            plugin.getGuiManager().sessionData.remove(player.getUniqueId());
            return;
        }

        if (item.getType() == nextMat && displayName.contains(nextName)) {
            playSoundSafe(player, plugin.getConfig().getString("sounds.click", "CLICK"));
            plugin.getGuiManager().openSkinSelectionGui(player);
            return;
        }

        if (item.getType() == Material.PAPER) {
            // It's a rank
            List<String> groups = plugin.getConfig().getStringList("gui.ranks.groups");
            String selectedGroup = null;
            
            // Very basic matching, assuming format "{prefix} {group_name}"
            for (String group : groups) {
                if (displayName.toLowerCase().contains(group.toLowerCase())) {
                    selectedGroup = group;
                    break;
                }
            }
            
            if (selectedGroup != null) {
                DisguiseData data = plugin.getGuiManager().sessionData.get(player.getUniqueId());
                if (data != null) {
                    data.setRank(selectedGroup);
                    playSoundSafe(player, plugin.getConfig().getString("sounds.click", "CLICK"));
                    player.sendMessage(plugin.color("&aSelected rank: " + selectedGroup));
                }
            }
        }
    }

    private void handleSkinSelection(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        String displayName = ChatColor.stripColor(meta.getDisplayName());

        Material customMat = Material.valueOf(plugin.getConfig().getString("gui.skins.custom_name_button.material"));
        String customNameStr = ChatColor.stripColor(plugin.color(plugin.getConfig().getString("gui.skins.custom_name_button.name")));
        
        if (item.getType() == customMat && displayName.contains(customNameStr)) {
            player.closeInventory();
            plugin.getGuiManager().awaitingChatInput.put(player.getUniqueId(), true);
            player.sendMessage(plugin.color(plugin.getConfig().getString("messages.type_name_in_chat")));
            playSoundSafe(player, plugin.getConfig().getString("sounds.click", "CLICK"));
            return;
        }
        
        // It's a head
        if (item.getType() == Material.SKULL_ITEM) {
            DisguiseData data = plugin.getGuiManager().sessionData.get(player.getUniqueId());
            if (data != null) {
                data.setName(displayName);
                // In a real implementation you would extract the texture string from the skull meta
                data.setSkin("texture_placeholder"); 
                playSoundSafe(player, plugin.getConfig().getString("sounds.click", "CLICK"));
                plugin.getGuiManager().openConfirmGui(player);
            }
        }
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

    private void handleConfirmation(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        String displayName = ChatColor.stripColor(meta.getDisplayName());

        Material confirmMat = Material.valueOf(plugin.getConfig().getString("gui.confirm.confirm_button.material"));
        Material cancelMat = Material.valueOf(plugin.getConfig().getString("gui.confirm.cancel_button.material"));
        
        String confirmNameStr = ChatColor.stripColor(plugin.color(plugin.getConfig().getString("gui.confirm.confirm_button.name")));
        String cancelNameStr = ChatColor.stripColor(plugin.color(plugin.getConfig().getString("gui.confirm.cancel_button.name")));

        if (item.getType() == cancelMat && displayName.contains(cancelNameStr)) {
            player.closeInventory();
            playSoundSafe(player, plugin.getConfig().getString("sounds.cancel", "NOTE_BASS"));
            plugin.getGuiManager().sessionData.remove(player.getUniqueId());
            player.sendMessage(plugin.color(plugin.getConfig().getString("messages.cancel_disguise")));
            return;
        }

        if (item.getType() == confirmMat && displayName.contains(confirmNameStr)) {
            DisguiseData data = plugin.getGuiManager().sessionData.get(player.getUniqueId());
            if (data != null && data.getName() != null && data.getRank() != null) {
                player.closeInventory();
                plugin.getDatabaseManager().saveDisguiseData(player.getUniqueId(), data);
                
                String msg = plugin.getConfig().getString("messages.disguised")
                        .replace("{name}", data.getName())
                        .replace("{rank}", data.getRank());
                
                changeName(player, data.getName());
                player.sendMessage(plugin.color(msg));
                playSoundSafe(player, plugin.getConfig().getString("sounds.success", "LEVEL_UP"));
                
                plugin.getGuiManager().sessionData.remove(player.getUniqueId());
            }
        }
    }
}
