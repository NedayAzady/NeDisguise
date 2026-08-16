package me.nedayazady.listeners;

import me.nedayazady.NeDisguise;
import me.nedayazady.database.DisguiseData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
        String title = event.getInventory().getTitle();
        ItemStack currentItem = event.getCurrentItem();

        if (currentItem == null || currentItem.getType() == Material.AIR) return;

        String rankTitle = plugin.color(plugin.getConfig().getString("gui.ranks.title"));
        String skinTitle = plugin.color(plugin.getConfig().getString("gui.skins.title"));
        String confirmTitle = plugin.color(plugin.getConfig().getString("gui.confirm.title"));

        if (title.equals(rankTitle)) {
            event.setCancelled(true);
            handleRankSelection(player, currentItem);
        } else if (title.equals(skinTitle)) {
            event.setCancelled(true);
            handleSkinSelection(player, currentItem);
        } else if (title.equals(confirmTitle)) {
            event.setCancelled(true);
            handleConfirmation(player, currentItem);
        }
    }

    private void handleRankSelection(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        String displayName = ChatColor.stripColor(meta.getDisplayName());
        
        Material nextMat = Material.valueOf(plugin.getConfig().getString("gui.ranks.next_button.material"));
        Material cancelMat = Material.valueOf(plugin.getConfig().getString("gui.ranks.cancel_button.material"));

        if (item.getType() == cancelMat && displayName.equalsIgnoreCase(ChatColor.stripColor(plugin.color(plugin.getConfig().getString("gui.ranks.cancel_button.name"))))) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.cancel", "BLOCK_NOTE_BLOCK_BASS")), 1f, 1f);
            plugin.getGuiManager().sessionData.remove(player.getUniqueId());
            return;
        }

        if (item.getType() == nextMat && displayName.equalsIgnoreCase(ChatColor.stripColor(plugin.color(plugin.getConfig().getString("gui.ranks.next_button.name"))))) {
            player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.click", "UI_BUTTON_CLICK")), 1f, 1f);
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
                    player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.click", "UI_BUTTON_CLICK")), 1f, 1f);
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
        if (item.getType() == customMat && displayName.equalsIgnoreCase(ChatColor.stripColor(plugin.color(plugin.getConfig().getString("gui.skins.custom_name_button.name"))))) {
            player.closeInventory();
            plugin.getGuiManager().awaitingChatInput.put(player.getUniqueId(), true);
            player.sendMessage(plugin.color(plugin.getConfig().getString("messages.type_name_in_chat")));
            player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.click", "UI_BUTTON_CLICK")), 1f, 1f);
            return;
        }
        
        // It's a head
        if (item.getType() == Material.SKULL_ITEM) {
            DisguiseData data = plugin.getGuiManager().sessionData.get(player.getUniqueId());
            if (data != null) {
                data.setName(displayName);
                // In a real implementation you would extract the texture string from the skull meta
                data.setSkin("texture_placeholder"); 
                player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.click", "UI_BUTTON_CLICK")), 1f, 1f);
                plugin.getGuiManager().openConfirmGui(player);
            }
        }
    }

    private void handleConfirmation(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        String displayName = ChatColor.stripColor(meta.getDisplayName());

        Material confirmMat = Material.valueOf(plugin.getConfig().getString("gui.confirm.confirm_button.material"));
        Material cancelMat = Material.valueOf(plugin.getConfig().getString("gui.confirm.cancel_button.material"));

        if (item.getType() == cancelMat) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.cancel", "BLOCK_NOTE_BLOCK_BASS")), 1f, 1f);
            plugin.getGuiManager().sessionData.remove(player.getUniqueId());
            player.sendMessage(plugin.color(plugin.getConfig().getString("messages.cancel_disguise")));
            return;
        }

        if (item.getType() == confirmMat) {
            DisguiseData data = plugin.getGuiManager().sessionData.get(player.getUniqueId());
            if (data != null && data.getName() != null && data.getRank() != null) {
                player.closeInventory();
                plugin.getDatabaseManager().saveDisguiseData(player.getUniqueId(), data);
                
                String msg = plugin.getConfig().getString("messages.disguised")
                        .replace("{name}", data.getName())
                        .replace("{rank}", data.getRank());
                
                player.sendMessage(plugin.color(msg));
                player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.success", "ENTITY_PLAYER_LEVELUP")), 1f, 1f);
                
                plugin.getGuiManager().sessionData.remove(player.getUniqueId());
            }
        }
    }
}
