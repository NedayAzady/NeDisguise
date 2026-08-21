package me.nedayazady.gui;

import me.nedayazady.NeDisguise;
import me.nedayazady.database.DisguiseData;
import me.nedayazady.utils.ItemBuilder;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
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
    public final Map<UUID, Integer> rankPage = new HashMap<>();
    public final Map<UUID, Integer> namePage = new HashMap<>();
    public final Map<UUID, Integer> skinPage = new HashMap<>();

    public GuiManager(NeDisguise plugin) {
        this.plugin = plugin;
    }

    private void fillGui(Inventory inv) {
        Material fillerMat = Material.STAINED_GLASS_PANE;
        short fillerData = 15; // Black glass
        ItemStack filler = new ItemBuilder(fillerMat, 1, fillerData).name(" ").build();
        
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                inv.setItem(i, filler);
            }
        }
    }

    public void openSetupGui(Player player) {
        FileConfiguration config = plugin.getConfigManager().getSetupGuiConfig();
        String title = plugin.color(config.getString("title", "Disguise: Setup"));
        int size = config.getInt("size", 54);

        Inventory inv = Bukkit.createInventory(null, size, title);

        DisguiseData data = sessionData.computeIfAbsent(player.getUniqueId(), k -> new DisguiseData(player.getName(), null, null, null));

        // Decorative Compass
        if (config.contains("items.compass")) {
            int slot = config.getInt("items.compass.slot", 4);
            inv.setItem(slot, new ItemBuilder(Material.COMPASS).name(plugin.color(config.getString("items.compass.name", " "))).build());
        }

        // History Heads / Decorative Heads
        if (config.contains("items.history_heads")) {
            List<Integer> slots = config.getIntegerList("items.history_heads.slots");
            String texture = config.getString("items.history_heads.texture");
            String name = plugin.color(config.getString("items.history_heads.name", "&8?"));
            for (int slot : slots) {
                inv.setItem(slot, new ItemBuilder(Material.SKULL_ITEM, 1, (short) 3).name(name).setSkullTexture(texture).build());
            }
        }

        // Status Button (Middle)
        if (config.contains("items.status")) {
            int slot = config.getInt("items.status.slot", 31);
            Material mat = Material.valueOf(config.getString("items.status.material", "EMERALD"));
            String name = plugin.color(config.getString("items.status.name", "&aApply disguise"));
            
            String curRank = data.getRank() != null ? data.getRank() : "random on apply";
            String curName = data.getName() != null ? data.getName() : "random on apply";
            String curSkin = data.getSkin() != null ? (data.getSkin().length() > 16 ? "Custom Skin" : data.getSkin()) : "random on apply";

            List<String> lore = config.getStringList("items.status.lore").stream()
                    .map(l -> plugin.color(l.replace("{rank}", curRank).replace("{name}", curName).replace("{skin}", curSkin)))
                    .collect(Collectors.toList());

            inv.setItem(slot, new ItemBuilder(mat).name(name).lore(lore).build());
        }

        // Close Button
        if (config.contains("items.close")) {
            int slot = config.getInt("items.close.slot", 45);
            inv.setItem(slot, new ItemBuilder(Material.BARRIER).name(plugin.color(config.getString("items.close.name", "&cClose"))).build());
        }

        // Rank Menu Button
        if (config.contains("items.rank_menu")) {
            int slot = config.getInt("items.rank_menu.slot", 48);
            inv.setItem(slot, new ItemBuilder(Material.PAPER).name(plugin.color(config.getString("items.rank_menu.name", "&bSelect Rank"))).build());
        }

        // Name Menu Button
        if (config.contains("items.name_menu")) {
            int slot = config.getInt("items.name_menu.slot", 49);
            inv.setItem(slot, new ItemBuilder(Material.NAME_TAG).name(plugin.color(config.getString("items.name_menu.name", "&eSelect Name"))).build());
        }

        // Skin Menu Button
        if (config.contains("items.skin_menu")) {
            int slot = config.getInt("items.skin_menu.slot", 50);
            inv.setItem(slot, new ItemBuilder(Material.SKULL_ITEM, 1, (short) 3).name(plugin.color(config.getString("items.skin_menu.name", "&6Select Skin"))).build());
        }

        fillGui(inv);
        player.openInventory(inv);
    }

    public void openRankGui(Player player) {
        FileConfiguration config = plugin.getConfigManager().getRankGuiConfig();
        String title = plugin.color(config.getString("title", "Disguise: Rank"));
        int size = config.getInt("size", 54);

        Inventory inv = Bukkit.createInventory(null, size, title);

        DisguiseData data = sessionData.computeIfAbsent(player.getUniqueId(), k -> new DisguiseData(player.getName(), null, null, null));

        // Decorative Compass
        if (config.contains("items.compass")) {
            int slot = config.getInt("items.compass.slot", 4);
            inv.setItem(slot, new ItemBuilder(Material.COMPASS).name(plugin.color(config.getString("items.compass.name", " "))).build());
        }

        // Status Item (Current disguise head/paper)
        if (config.contains("items.status")) {
            int slot = config.getInt("items.status.slot", 4);
            Material mat = Material.valueOf(config.getString("items.status.material", "PAPER"));
            String name = plugin.color(config.getString("items.status.name", "&aCurrent disguise"));
            
            String statusStr = data.getRank() != null ? "&aDisguised" : "&cnot disguised";
            String curName = data.getName() != null ? data.getName() : "none";
            String curRank = data.getRank() != null ? data.getRank() : "none";
            String curSkin = data.getSkin() != null ? (data.getSkin().length() > 16 ? "Custom Skin" : data.getSkin()) : "none";

            List<String> lore = config.getStringList("items.status.lore").stream()
                    .map(l -> plugin.color(l.replace("{real_name}", player.getName())
                            .replace("{status}", statusStr)
                            .replace("{name}", curName)
                            .replace("{rank}", curRank)
                            .replace("{skin}", curSkin)))
                    .collect(Collectors.toList());

            inv.setItem(slot, new ItemBuilder(mat).name(name).lore(lore).build());
        }

        // Ranks
        List<String> groups = config.getStringList("ranks.groups");
        List<Integer> slots = config.getIntegerList("ranks.slots");
        
        for (int i = 0; i < groups.size() && i < slots.size(); i++) {
            final String groupName = groups.get(i);
            int slot = slots.get(i);

            Group group = plugin.getLuckPerms().getGroupManager().getGroup(groupName);
            String prefixVal = group != null ? group.getCachedData().getMetaData().getPrefix() : "";
            if (prefixVal == null) prefixVal = "";
            final String prefix = prefixVal;

            Material mat = Material.valueOf(config.getString("ranks.rank_item.material", "PAPER"));
            String name = plugin.color(config.getString("ranks.rank_item.name", "&bRank: &f{group_name}")
                    .replace("{group_name}", groupName)
                    .replace("{prefix}", prefix));

            List<String> lore = config.getStringList("ranks.rank_item.lore").stream()
                    .map(l -> plugin.color(l.replace("{group_name}", groupName).replace("{prefix}", prefix)))
                    .collect(Collectors.toList());

            inv.setItem(slot, new ItemBuilder(mat).name(name).lore(lore).build());
        }

        // Back Button
        if (config.contains("items.back")) {
            int slot = config.getInt("items.back.slot", 45);
            inv.setItem(slot, new ItemBuilder(Material.BARRIER).name(plugin.color(config.getString("items.back.name", "&cBack"))).build());
        }

        // Unnick Button (Slot 53)
        if (config.contains("items.unnick")) {
            int slot = config.getInt("items.unnick.slot", 53);
            inv.setItem(slot, new ItemBuilder(Material.REDSTONE_BLOCK).name(plugin.color(config.getString("items.unnick.name", "&cReset Disguise"))).build());
        }

        fillGui(inv);
        player.openInventory(inv);
    }

    public void openNameGui(Player player) {
        FileConfiguration config = plugin.getConfigManager().getNameGuiConfig();
        String title = plugin.color(config.getString("title", "Disguise: Name"));
        int size = config.getInt("size", 54);

        Inventory inv = Bukkit.createInventory(null, size, title);

        DisguiseData data = sessionData.computeIfAbsent(player.getUniqueId(), k -> new DisguiseData(player.getName(), null, null, null));

        // Decorative Compass
        if (config.contains("items.compass")) {
            int slot = config.getInt("items.compass.slot", 4);
            inv.setItem(slot, new ItemBuilder(Material.COMPASS).name(plugin.color(config.getString("items.compass.name", " "))).build());
        }

        // History Heads / Decorative Heads
        if (config.contains("items.history_heads")) {
            List<Integer> slots = config.getIntegerList("items.history_heads.slots");
            String texture = config.getString("items.history_heads.texture");
            String name = plugin.color(config.getString("items.history_heads.name", "&8?"));
            for (int slot : slots) {
                inv.setItem(slot, new ItemBuilder(Material.SKULL_ITEM, 1, (short) 3).name(name).setSkullTexture(texture).build());
            }
        }

        // Custom Name Button
        if (config.contains("items.custom_name")) {
            int slot = config.getInt("items.custom_name.slot", 30);
            inv.setItem(slot, new ItemBuilder(Material.NAME_TAG)
                    .name(plugin.color(config.getString("items.custom_name.name", "&eCustom Name")))
                    .lore(config.getStringList("items.custom_name.lore").stream().map(plugin::color).collect(Collectors.toList()))
                    .build());
        }

        // Random Name Button
        if (config.contains("items.random_name")) {
            int slot = config.getInt("items.random_name.slot", 32);
            String curName = data.getName() != null ? data.getName() : "None";
            inv.setItem(slot, new ItemBuilder(Material.PAPER)
                    .name(plugin.color(config.getString("items.random_name.name", "&dRandom Name")))
                    .lore(config.getStringList("items.random_name.lore").stream()
                            .map(l -> plugin.color(l.replace("{name}", curName)))
                            .collect(Collectors.toList()))
                    .build());
        }

        // Back Button
        if (config.contains("items.back")) {
            int slot = config.getInt("items.back.slot", 45);
            inv.setItem(slot, new ItemBuilder(Material.BARRIER).name(plugin.color(config.getString("items.back.name", "&cBack"))).build());
        }

        fillGui(inv);
        player.openInventory(inv);
    }

    public void openSkinGui(Player player) {
        FileConfiguration config = plugin.getConfigManager().getSkinGuiConfig();
        String title = plugin.color(config.getString("title", "Disguise: Skin"));
        int size = config.getInt("size", 54);

        Inventory inv = Bukkit.createInventory(null, size, title);

        DisguiseData data = sessionData.computeIfAbsent(player.getUniqueId(), k -> new DisguiseData(player.getName(), null, null, null));

        // Decorative Compass
        if (config.contains("items.compass")) {
            int slot = config.getInt("items.compass.slot", 4);
            inv.setItem(slot, new ItemBuilder(Material.COMPASS).name(plugin.color(config.getString("items.compass.name", " "))).build());
        }

        // History Heads / Decorative Heads
        if (config.contains("items.history_heads")) {
            List<Integer> slots = config.getIntegerList("items.history_heads.slots");
            String texture = config.getString("items.history_heads.texture");
            String name = plugin.color(config.getString("items.history_heads.name", "&8?"));
            for (int slot : slots) {
                inv.setItem(slot, new ItemBuilder(Material.SKULL_ITEM, 1, (short) 3).name(name).setSkullTexture(texture).build());
            }
        }

        // Random Skin Button
        if (config.contains("items.random_skin")) {
            int slot = config.getInt("items.random_skin.slot", 31);
            String curSkin = data.getSkin() != null ? (data.getSkin().length() > 16 ? "Custom Skin" : data.getSkin()) : "None";
            inv.setItem(slot, new ItemBuilder(Material.SKULL_ITEM, 1, (short) 3)
                    .name(plugin.color(config.getString("items.random_skin.name", "&dRandom Skin")))
                    .lore(config.getStringList("items.random_skin.lore").stream()
                            .map(l -> plugin.color(l.replace("{skin_name}", curSkin)))
                            .collect(Collectors.toList()))
                    .build());
        }

        // Back Button
        if (config.contains("items.back")) {
            int slot = config.getInt("items.back.slot", 45);
            inv.setItem(slot, new ItemBuilder(Material.BARRIER).name(plugin.color(config.getString("items.back.name", "&cBack"))).build());
        }

        fillGui(inv);
        player.openInventory(inv);
    }
}
