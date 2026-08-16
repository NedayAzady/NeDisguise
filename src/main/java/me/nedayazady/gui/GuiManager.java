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

    public void openRankSelectionGui(Player player) {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("gui.ranks");
        String title = plugin.color(config.getString("title"));
        int size = config.getInt("size");

        Inventory inv = Bukkit.createInventory(null, size, title);

        List<String> groups = config.getStringList("groups");
        int slot = 10;
        for (String groupName : groups) {
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

        ConfigurationSection nextBtn = config.getConfigurationSection("next_button");
        inv.setItem(nextBtn.getInt("slot"), new ItemBuilder(Material.valueOf(nextBtn.getString("material")))
                .name(plugin.color(nextBtn.getString("name"))).build());

        ConfigurationSection cancelBtn = config.getConfigurationSection("cancel_button");
        inv.setItem(cancelBtn.getInt("slot"), new ItemBuilder(Material.valueOf(cancelBtn.getString("material")))
                .name(plugin.color(cancelBtn.getString("name"))).build());

        fillGui(inv, config);
        player.openInventory(inv);
        
        // Initialize session
        sessionData.put(player.getUniqueId(), new DisguiseData(null, null, null));
    }

    public void openSkinSelectionGui(Player player) {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("gui.skins");
        String title = plugin.color(config.getString("title"));
        int size = config.getInt("size");

        Inventory inv = Bukkit.createInventory(null, size, title);

        List<Map<?, ?>> skins = config.getMapList("available_skins");
        int slot = 10;
        
        for (Map<?, ?> skinInfo : skins) {
            String name = (String) skinInfo.get("name");
            String texture = (String) skinInfo.get("texture");
            
            // SKULL_ITEM with data 3 is player head in 1.8
            ItemBuilder head = new ItemBuilder(Material.SKULL_ITEM, 1, (short) 3)
                    .name(plugin.color("&e" + name))
                    .setSkullTexture(texture);
                    
            inv.setItem(slot++, head.build());
            if (slot == 17 || slot == 26 || slot == 35) slot += 2;
        }

        ConfigurationSection customBtn = config.getConfigurationSection("custom_name_button");
        inv.setItem(customBtn.getInt("slot"), new ItemBuilder(Material.valueOf(customBtn.getString("material")))
                .name(plugin.color(customBtn.getString("name")))
                .lore(customBtn.getStringList("lore").stream().map(plugin::color).collect(Collectors.toList()))
                .build());

        fillGui(inv, config);
        player.openInventory(inv);
    }

    public void openConfirmGui(Player player) {
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
