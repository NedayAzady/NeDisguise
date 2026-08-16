package me.nedayazady.listeners;

import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.SignGUIAction;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
        
        Material nextMat = Material.valueOf(plugin.getConfig().getString("gui.ranks.next_button.material", "ARROW"));
        Material prevMat = Material.valueOf(plugin.getConfig().getString("gui.ranks.previous_button.material", "ARROW"));
        Material cancelMat = Material.valueOf(plugin.getConfig().getString("gui.ranks.cancel_button.material"));
        Material unnickMat = Material.valueOf(plugin.getConfig().getString("gui.ranks.unnick_button.material", "BARRIER"));
        
        String nextName = ChatColor.stripColor(plugin.color(plugin.getConfig().getString("gui.ranks.next_button.name", "Next")));
        String prevName = ChatColor.stripColor(plugin.color(plugin.getConfig().getString("gui.ranks.previous_button.name", "Previous")));
        String cancelName = ChatColor.stripColor(plugin.color(plugin.getConfig().getString("gui.ranks.cancel_button.name")));
        String unnickName = ChatColor.stripColor(plugin.color(plugin.getConfig().getString("gui.ranks.unnick_button.name", "Reset Disguise")));

        if (item.getType() == unnickMat && displayName.contains(unnickName)) {
            player.closeInventory();
            playSoundSafe(player, plugin.getConfig().getString("sounds.success", "LEVEL_UP"));
            plugin.getGuiManager().sessionData.remove(player.getUniqueId());
            plugin.getDatabaseManager().removeDisguiseData(player.getUniqueId());
            
            // Execute configured commands to update TAB/Tags plugins
            List<String> commands = plugin.getConfig().getStringList("on_undisguise_commands");
            for (String cmd : commands) {
                String originalName = player.getName();
                String formattedCmd = cmd.replace("{player}", originalName);
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), formattedCmd);
            }
            
            // Revert name visually using the PlayerListener instance method to keep it central
            // Pass empty string for rank since it's an unnick
            PlayerListener playerListener = new PlayerListener(plugin);
            playerListener.changeName(player, player.getName(), "");
            player.sendMessage(plugin.color(plugin.getConfig().getString("messages.undisguised")));
            return;
        }

        if (item.getType() == cancelMat && displayName.contains(cancelName)) {
            player.closeInventory();
            playSoundSafe(player, plugin.getConfig().getString("sounds.cancel", "NOTE_BASS"));
            plugin.getGuiManager().sessionData.remove(player.getUniqueId());
            return;
        }

        if (item.getType() == nextMat && displayName.contains(nextName)) {
            playSoundSafe(player, plugin.getConfig().getString("sounds.click", "CLICK"));
            int page = plugin.getGuiManager().rankPage.getOrDefault(player.getUniqueId(), 1);
            plugin.getGuiManager().rankPage.put(player.getUniqueId(), page + 1);
            plugin.getGuiManager().openRankSelectionGui(player);
            return;
        }
        
        if (item.getType() == prevMat && displayName.contains(prevName)) {
            playSoundSafe(player, plugin.getConfig().getString("sounds.click", "CLICK"));
            int page = plugin.getGuiManager().rankPage.getOrDefault(player.getUniqueId(), 1);
            plugin.getGuiManager().rankPage.put(player.getUniqueId(), page - 1);
            plugin.getGuiManager().openRankSelectionGui(player);
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
                    plugin.getGuiManager().openSkinSelectionGui(player);
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
        
        Material randomMat = Material.valueOf(plugin.getConfig().getString("gui.skins.random_name_button.material", "COMMAND"));
        String randomNameStr = ChatColor.stripColor(plugin.color(plugin.getConfig().getString("gui.skins.random_name_button.name", "Random Name")));
        
        Material nextMat = Material.valueOf(plugin.getConfig().getString("gui.skins.next_button.material", "ARROW"));
        Material prevMat = Material.valueOf(plugin.getConfig().getString("gui.skins.previous_button.material", "ARROW"));
        Material backMat = Material.valueOf(plugin.getConfig().getString("gui.skins.back_button.material", "BARRIER"));
        
        String nextName = ChatColor.stripColor(plugin.color(plugin.getConfig().getString("gui.skins.next_button.name", "Next")));
        String prevName = ChatColor.stripColor(plugin.color(plugin.getConfig().getString("gui.skins.previous_button.name", "Previous")));
        String backName = ChatColor.stripColor(plugin.color(plugin.getConfig().getString("gui.skins.back_button.name", "Back")));

        if (item.getType() == nextMat && displayName.contains(nextName)) {
            playSoundSafe(player, plugin.getConfig().getString("sounds.click", "CLICK"));
            int page = plugin.getGuiManager().skinPage.getOrDefault(player.getUniqueId(), 1);
            plugin.getGuiManager().skinPage.put(player.getUniqueId(), page + 1);
            plugin.getGuiManager().openSkinSelectionGui(player);
            return;
        }
        
        if (item.getType() == prevMat && displayName.contains(prevName)) {
            playSoundSafe(player, plugin.getConfig().getString("sounds.click", "CLICK"));
            int page = plugin.getGuiManager().skinPage.getOrDefault(player.getUniqueId(), 1);
            plugin.getGuiManager().skinPage.put(player.getUniqueId(), page - 1);
            plugin.getGuiManager().openSkinSelectionGui(player);
            return;
        }
        
        if (item.getType() == backMat && displayName.contains(backName)) {
            playSoundSafe(player, plugin.getConfig().getString("sounds.click", "CLICK"));
            plugin.getGuiManager().openRankSelectionGui(player);
            return;
        }

        if (item.getType() == randomMat && displayName.contains(randomNameStr)) {
            player.closeInventory();
            playSoundSafe(player, plugin.getConfig().getString("sounds.click", "CLICK"));
            
            // Get random name and skin
            java.util.List<java.util.Map<?, ?>> skins = plugin.getConfig().getMapList("gui.skins.available_skins");
            
            // Filter out the random button from the list itself if it's there
            java.util.List<java.util.Map<?, ?>> actualSkins = new java.util.ArrayList<>();
            for (java.util.Map<?, ?> skinMap : skins) {
                if (!skinMap.containsKey("is_random") || !(Boolean)skinMap.get("is_random")) {
                    actualSkins.add(skinMap);
                }
            }
            
            if (!actualSkins.isEmpty()) {
                int randomIndex = new java.util.Random().nextInt(actualSkins.size());
                java.util.Map<?, ?> randomSkin = actualSkins.get(randomIndex);
                
                String[] prefixes = {"Pro", "Noob", "xX", "The", "Epic", "Dark", "Ghost", "Ninja", "Super", "Mega", "Ultra", "Fast", "Iron", "Gold"};
                String[] suffixes = {"Gamer", "PVP", "Slayer", "Craft", "Boy", "Girl", "HD", "YT", "MC", "King", "Beast", "Master", "Lord"};
                String prefix = prefixes[(int) (Math.random() * prefixes.length)];
                String suffix = suffixes[(int) (Math.random() * suffixes.length)];
                String generatedName = prefix + suffix + (int)(Math.random() * 99);
                
                String generatedTexture = (String) randomSkin.get("texture");
                
                DisguiseData data = plugin.getGuiManager().sessionData.get(player.getUniqueId());
                if (data != null) {
                    data.setName(generatedName);
                    data.setSkin(generatedTexture);
                    Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openConfirmGui(player));
                }
            } else {
                player.sendMessage(plugin.color("&cNo skins configured for randomizer."));
            }
            return;
        }

        if (item.getType() == customMat && displayName.contains(customNameStr)) {
            player.closeInventory();
            playSoundSafe(player, plugin.getConfig().getString("sounds.click", "CLICK"));
            
            // Open Sign GUI for custom name input
            SignGUI.builder()
                    .setLines(new String[]{"", "^^^^^^^^^^^^^^^", "Enter a name", "for your disguise"})
                    .setType(Material.SIGN_POST)
                    .setHandler((p, result) -> {
                        String nameInput = result.getLineWithoutColor(0).trim();
                        
                        if (nameInput.isEmpty()) {
                            return Arrays.asList(SignGUIAction.run(() -> p.sendMessage(plugin.color("&cName cannot be empty."))));
                        }
                        
                        if (nameInput.length() > 16) {
                            return Arrays.asList(SignGUIAction.run(() -> p.sendMessage(plugin.color(plugin.getConfig().getString("messages.name_too_long")))));
                        }
                        
                        if (!nameInput.matches("^[a-zA-Z0-9_]+$")) {
                            return Arrays.asList(SignGUIAction.run(() -> p.sendMessage(plugin.color(plugin.getConfig().getString("messages.invalid_name")))));
                        }
                        
                        DisguiseData data = plugin.getGuiManager().sessionData.get(p.getUniqueId());
                        if (data != null) {
                            data.setName(nameInput);
                            return Arrays.asList(SignGUIAction.run(() -> {
                                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openConfirmGui(p));
                            }));
                        }
                        return Arrays.asList();
                    })
                    .build()
                    .open(player);
            return;
        }
        
        // It's a head
        if (item.getType() == Material.SKULL_ITEM) {
            DisguiseData data = plugin.getGuiManager().sessionData.get(player.getUniqueId());
            if (data != null) {
                // If they clicked random, generate a random name and pick a random skin
                if (displayName.contains("Random Name")) {
                    String[] prefixes = {"Pro", "Noob", "xX", "The", "Epic", "Dark", "Ghost", "Ninja", "Super", "Mega", "Ultra", "Fast", "Iron", "Gold"};
                    String[] suffixes = {"Gamer", "PVP", "Slayer", "Craft", "Boy", "Girl", "HD", "YT", "MC", "King", "Beast", "Master", "Lord"};
                    String prefix = prefixes[(int) (Math.random() * prefixes.length)];
                    String suffix = suffixes[(int) (Math.random() * suffixes.length)];
                    String randomName = prefix + suffix + (int)(Math.random() * 99);
                    
                    data.setName(randomName);
                    
                    // Assign random texture from config
                    List<Map<?, ?>> skins = plugin.getConfig().getMapList("gui.skins.available_skins");
                    // Collect valid skins (not the random button itself)
                    java.util.List<Map<?, ?>> actualSkins = new java.util.ArrayList<>();
                    for (Map<?, ?> skinMap : skins) {
                        if (!skinMap.containsKey("is_random") || !(Boolean)skinMap.get("is_random")) {
                            actualSkins.add(skinMap);
                        }
                    }
                    if (!actualSkins.isEmpty()) {
                        Map<?, ?> randomSkin = actualSkins.get(new java.util.Random().nextInt(actualSkins.size()));
                        data.setSkin((String) randomSkin.get("texture"));
                    } else {
                        data.setSkin("texture_placeholder");
                    }
                } else {
                    data.setName(displayName);
                    // Get texture from the clicked item
                    String foundTexture = "texture_placeholder";
                    List<Map<?, ?>> skins = plugin.getConfig().getMapList("gui.skins.available_skins");
                    for (Map<?, ?> skinInfo : skins) {
                        if (skinInfo.get("name").equals(displayName)) {
                            foundTexture = (String) skinInfo.get("texture");
                            break;
                        }
                    }
                    data.setSkin(foundTexture); 
                }
                
                playSoundSafe(player, plugin.getConfig().getString("sounds.click", "CLICK"));
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
                
                PlayerListener playerListener = new PlayerListener(plugin);
                playerListener.changeName(player, data.getName(), data.getRank());
                player.sendMessage(plugin.color(msg));
                playSoundSafe(player, plugin.getConfig().getString("sounds.success", "LEVEL_UP"));
                
                // Reset pages for next time
                plugin.getGuiManager().rankPage.put(player.getUniqueId(), 1);
                plugin.getGuiManager().skinPage.put(player.getUniqueId(), 1);
                plugin.getGuiManager().sessionData.remove(player.getUniqueId());
            }
        }
    }
}
