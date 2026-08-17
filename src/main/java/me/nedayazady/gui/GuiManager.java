package me.nedayazady.gui;

import me.nedayazady.NeDisguise;
import me.nedayazady.database.DisguiseData;
import me.nedayazady.utils.ItemBuilder;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class GuiManager {

    private final NeDisguise plugin;
    public final Map<UUID, DisguiseData> sessionData = new HashMap<>();
    public final Map<UUID, Boolean> awaitingChatInput = new HashMap<>();
    
    // Pagination tracking
    public final Map<UUID, Integer> rankPage = new HashMap<>();
    public final Map<UUID, Integer> skinPage = new HashMap<>();

    public GuiManager(NeDisguise plugin) {
        this.plugin = plugin;
    }

    private void fillGui(Inventory inv, ConfigurationSection config) {
        Material fillerMat = Material.valueOf(plugin.getConfig().getString("gui.filler.material", "STAINED_GLASS_PANE"));
        short fillerData = (short) plugin.getConfig().getInt("gui.filler.data", 15);
        String fillerName = plugin.color(plugin.getConfig().getString("gui.filler.name", " "));
        
        ItemStack filler = new ItemBuilder(fillerMat, 1, fillerData).name(fillerName).build();
        
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                inv.setItem(i, filler);
            }
        }
    }

    public void openMainMenu(Player player) {
        // Main menu with 3 options: Rank, Name, Skin + Profile Info
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("gui.main");
        if (config == null) {
            // Fallback default
            Inventory inv = Bukkit.createInventory(null, 45, plugin.color("&8Disguise Menu"));
            fillGui(inv, plugin.getConfig().getConfigurationSection("gui"));
            
            // Just open rank selection for now if main is missing
            openRankSelectionGui(player);
            return;
        }
        
        String title = plugin.color(config.getString("title", "&8Disguise Menu"));
        int size = config.getInt("size", 45);
        Inventory inv = Bukkit.createInventory(null, size, title);

        DisguiseData data = sessionData.computeIfAbsent(player.getUniqueId(), k -> new DisguiseData(player.getName(), null, null, null));

        // Rank Button
        ConfigurationSection rankBtn = config.getConfigurationSection("rank_button");
        if (rankBtn != null) {
            inv.setItem(rankBtn.getInt("slot", 11), new ItemBuilder(Material.valueOf(rankBtn.getString("material", "PAPER")))
                .name(plugin.color(rankBtn.getString("name", "&aChange Rank")))
                .lore(rankBtn.getStringList("lore").stream().map(plugin::color).collect(Collectors.toList()))
                .build());
        }

        // Name Button
        ConfigurationSection nameBtn = config.getConfigurationSection("name_button");
        if (nameBtn != null) {
            inv.setItem(nameBtn.getInt("slot", 13), new ItemBuilder(Material.valueOf(nameBtn.getString("material", "NAME_TAG")))
                .name(plugin.color(nameBtn.getString("name", "&aChange Name")))
                .lore(nameBtn.getStringList("lore").stream().map(plugin::color).collect(Collectors.toList()))
                .build());
        }

        // Skin Button
        ConfigurationSection skinBtn = config.getConfigurationSection("skin_button");
        if (skinBtn != null) {
            inv.setItem(skinBtn.getInt("slot", 15), new ItemBuilder(Material.valueOf(skinBtn.getString("material", "SKULL_ITEM")))
                .name(plugin.color(skinBtn.getString("name", "&aChange Skin")))
                .lore(skinBtn.getStringList("lore").stream().map(plugin::color).collect(Collectors.toList()))
                .build());
        }

        // Unnick Button
        ConfigurationSection unnickBtn = config.getConfigurationSection("unnick_button");
        if (unnickBtn != null) {
            inv.setItem(unnickBtn.getInt("slot", 31), new ItemBuilder(Material.valueOf(unnickBtn.getString("material", "BARRIER")))
                .name(plugin.color(unnickBtn.getString("name", "&cReset Disguise")))
                .lore(unnickBtn.getStringList("lore").stream().map(plugin::color).collect(Collectors.toList()))
                .build());
        }
        
        // Disguise Button
        ConfigurationSection disguiseBtn = config.getConfigurationSection("disguise_button");
        if (disguiseBtn != null) {
            inv.setItem(disguiseBtn.getInt("slot", 40), new ItemBuilder(Material.valueOf(disguiseBtn.getString("material", "EMERALD_BLOCK")))
                .name(plugin.color(disguiseBtn.getString("name", "&aApply Disguise")))
                .build());
        }

        // Status Head
        ConfigurationSection statusBtn = config.getConfigurationSection("status_button");
        if (statusBtn != null) {
            String skullTexture = data.getSkin() != null && !data.getSkin().equals("texture_placeholder") ? data.getSkin() : null;
            String currentName = data.getName() != null ? data.getName() : "None";
            String currentRank = data.getRank() != null ? data.getRank() : "None";
            
            ItemBuilder infoHead = new ItemBuilder(Material.SKULL_ITEM, 1, (short) 3)
                .name(plugin.color(statusBtn.getString("name", "&eCurrent Status")))
                .lore(java.util.Arrays.asList(
                    plugin.color("&7Name: &f" + currentName),
                    plugin.color("&7Rank: &f" + currentRank)
                ));
            
            if (skullTexture != null) {
                infoHead.setSkullTexture(skullTexture);
            } else {
                infoHead.setSkullOwner(player.getName()); // Default to their real head
            }
                
            inv.setItem(statusBtn.getInt("slot", 4), infoHead.build());
        }

        fillGui(inv, config);
        player.openInventory(inv);
    }

    public void openRankSelectionGui(Player player) {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("gui.ranks");
        String title = plugin.color(config.getString("title"));
        int size = config.getInt("size");

        Inventory inv = Bukkit.createInventory(null, size, title);

        List<String> groups = config.getStringList("groups");
        
        int itemsPerPage = 21;
        int maxPages = (int) Math.ceil((double) groups.size() / itemsPerPage);
        if (maxPages == 0) maxPages = 1;
        
        // Ensure page bounds
        if (page > maxPages) page = maxPages;
        if (page < 1) page = 1;
        rankPage.put(player.getUniqueId(), page);

        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, groups.size());

        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            String groupName = groups.get(i);
            Group group = plugin.getLuckPerms().getGroupManager().getGroup(groupName);
            String prefix = group != null ? group.getCachedData().getMetaData().getPrefix() : "";
            if (prefix == null) prefix = "";

            ConfigurationSection itemConfig = config.getConfigurationSection("rank_item");
            Material mat = Material.valueOf(itemConfig.getString("material", "PAPER"));
            String name = plugin.color(itemConfig.getString("name")
                    .replace("{prefix}", prefix)
                    .replace("{group_name}", groupName));
            
            List<String> lore = itemConfig.getStringList("lore").stream()
                    .map(plugin::color).collect(Collectors.toList());

            inv.setItem(slot++, new ItemBuilder(mat).name(name).lore(lore).build());
            
            if (slot == 17 || slot == 26 || slot == 35) slot += 2;
        }

        if (page < maxPages) {
            ConfigurationSection nextBtn = config.getConfigurationSection("next_button");
            inv.setItem(nextBtn.getInt("slot"), new ItemBuilder(Material.valueOf(nextBtn.getString("material")))
                    .name(plugin.color(nextBtn.getString("name"))).build());
        }
        
        if (page > 1) {
            ConfigurationSection prevBtn = config.getConfigurationSection("previous_button");
            inv.setItem(prevBtn.getInt("slot"), new ItemBuilder(Material.valueOf(prevBtn.getString("material")))
                    .name(plugin.color(prevBtn.getString("name"))).build());
        }
        
        ConfigurationSection pageBtn = config.getConfigurationSection("page_indicator");
        if (pageBtn != null) {
            String pageName = plugin.color(pageBtn.getString("name").replace("{page}", String.valueOf(page)).replace("{max}", String.valueOf(maxPages)));
            inv.setItem(pageBtn.getInt("slot"), new ItemBuilder(Material.valueOf(pageBtn.getString("material"))).name(pageName).build());
        }

        ConfigurationSection unnickBtn = config.getConfigurationSection("unnick_button");
        if (unnickBtn != null) {
            inv.setItem(unnickBtn.getInt("slot"), new ItemBuilder(Material.valueOf(unnickBtn.getString("material", "BARRIER")))
                    .name(plugin.color(unnickBtn.getString("name", "&cReset Disguise")))
                    .lore(unnickBtn.getStringList("lore").stream().map(plugin::color).collect(Collectors.toList()))
                    .build());
        }

        ConfigurationSection cancelBtn = config.getConfigurationSection("cancel_button");
        inv.setItem(cancelBtn.getInt("slot"), new ItemBuilder(Material.valueOf(cancelBtn.getString("material")))
                .name(plugin.color(cancelBtn.getString("name"))).build());

        fillGui(inv, config);
        player.openInventory(inv);
        
        // Initialize session if empty
        sessionData.putIfAbsent(player.getUniqueId(), new DisguiseData(player.getName(), null, null, null));
    }

    public void openSkinSelectionGui(Player player) {
        int page = skinPage.getOrDefault(player.getUniqueId(), 1);
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("gui.skins");
        String title = plugin.color(config.getString("title"));
        int size = config.getInt("size");

        Inventory inv = Bukkit.createInventory(null, size, title);

        List<Map<?, ?>> skins = config.getMapList("available_skins");
        
        int itemsPerPage = 21;
        int maxPages = (int) Math.ceil((double) skins.size() / itemsPerPage);
        if (maxPages == 0) maxPages = 1;
        
        if (page > maxPages) page = maxPages;
        if (page < 1) page = 1;
        skinPage.put(player.getUniqueId(), page);
        
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, skins.size());

        int slot = 10;
        
        for (int i = startIndex; i < endIndex; i++) {
            Map<?, ?> skinInfo = skins.get(i);
            String name = (String) skinInfo.get("name");
            String texture = (String) skinInfo.get("texture");
            
            // SKULL_ITEM with data 3 is player head in 1.8
            ItemBuilder head = new ItemBuilder(Material.SKULL_ITEM, 1, (short) 3)
                    .name(plugin.color("&e" + name));
            
            // If the map has "is_random": true, we might not set a texture, or set a specific one
            if (skinInfo.containsKey("is_random") && (Boolean)skinInfo.get("is_random")) {
               if (texture != null && !texture.isEmpty()) {
                   head.setSkullTexture(texture);
               }
            } else {
               head.setSkullTexture(texture);
            }
                    
            inv.setItem(slot++, head.build());
            if (slot == 17 || slot == 26 || slot == 35) slot += 2;
        }

        if (page < maxPages) {
            ConfigurationSection nextBtn = config.getConfigurationSection("next_button");
            if (nextBtn != null) {
                inv.setItem(nextBtn.getInt("slot"), new ItemBuilder(Material.valueOf(nextBtn.getString("material")))
                        .name(plugin.color(nextBtn.getString("name"))).build());
            }
        }
        
        if (page > 1) {
            ConfigurationSection prevBtn = config.getConfigurationSection("previous_button");
            if (prevBtn != null) {
                inv.setItem(prevBtn.getInt("slot"), new ItemBuilder(Material.valueOf(prevBtn.getString("material")))
                        .name(plugin.color(prevBtn.getString("name"))).build());
            }
        }
        
        ConfigurationSection pageBtn = config.getConfigurationSection("page_indicator");
        if (pageBtn != null) {
            String pageName = plugin.color(pageBtn.getString("name").replace("{page}", String.valueOf(page)).replace("{max}", String.valueOf(maxPages)));
            inv.setItem(pageBtn.getInt("slot"), new ItemBuilder(Material.valueOf(pageBtn.getString("material"))).name(pageName).build());
        }
        
        ConfigurationSection backBtn = config.getConfigurationSection("back_button");
        if (backBtn != null) {
            inv.setItem(backBtn.getInt("slot"), new ItemBuilder(Material.valueOf(backBtn.getString("material")))
                    .name(plugin.color(backBtn.getString("name"))).build());
        }

        ConfigurationSection customBtn = config.getConfigurationSection("custom_name_button");
        if (customBtn != null) {
            inv.setItem(customBtn.getInt("slot"), new ItemBuilder(Material.valueOf(customBtn.getString("material")))
                    .name(plugin.color(customBtn.getString("name")))
                    .lore(customBtn.getStringList("lore").stream().map(plugin::color).collect(Collectors.toList()))
                    .build());
        }
        
        ConfigurationSection randomBtn = config.getConfigurationSection("random_name_button");
        if (randomBtn != null) {
            inv.setItem(randomBtn.getInt("slot"), new ItemBuilder(Material.valueOf(randomBtn.getString("material", "COMMAND")))
                    .name(plugin.color(randomBtn.getString("name", "&dRandom Name")))
                    .lore(randomBtn.getStringList("lore").stream().map(plugin::color).collect(Collectors.toList()))
                    .build());
        }

        fillGui(inv, config);
        player.openInventory(inv);
    }

    public void openConfirmGui(Player player) {
        // Reset pages back to 1 for the next time they open the GUI
        rankPage.put(player.getUniqueId(), 1);
        skinPage.put(player.getUniqueId(), 1);
        
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("gui.confirm");
        String title = plugin.color(config.getString("title"));
        int size = config.getInt("size");

        Inventory inv = Bukkit.createInventory(null, size, title);

        ConfigurationSection confirmBtn = config.getConfigurationSection("confirm_button");
        inv.setItem(confirmBtn.getInt("slot"), new ItemBuilder(Material.valueOf(confirmBtn.getString("material")))
                .name(plugin.color(confirmBtn.getString("name"))).build());

        ConfigurationSection cancelBtn = config.getConfigurationSection("cancel_button");
        inv.setItem(cancelBtn.getInt("slot"), new ItemBuilder(Material.valueOf(cancelBtn.getString("material")))
                .name(plugin.color(cancelBtn.getString("name"))).build());
                
        // Show current selections in the middle
        DisguiseData data = sessionData.get(player.getUniqueId());
        if (data != null) {
            String skullTexture = data.getSkin() != null && !data.getSkin().equals("texture_placeholder") ? data.getSkin() : null;
            ItemBuilder infoHead = new ItemBuilder(Material.SKULL_ITEM, 1, (short) 3)
                .name(plugin.color("&eYour Disguise"))
                .lore(java.util.Arrays.asList(
                    plugin.color("&7Name: &f" + (data.getName() != null ? data.getName() : "&cNone")),
                    plugin.color("&7Rank: &f" + (data.getRank() != null ? data.getRank() : "&cNone"))
                ));
            
            if (skullTexture != null) {
                infoHead.setSkullTexture(skullTexture);
            }
                
            inv.setItem(31, infoHead.build());
        }

        fillGui(inv, config);
        player.openInventory(inv);
    }
}
