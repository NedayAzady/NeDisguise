package me.nedayazady.listeners;

import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.SignGUIAction;
import me.nedayazady.NeDisguise;
import me.nedayazady.database.DisguiseData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class GuiListener implements Listener {

    private final NeDisguise plugin;
    private final Random random = new Random();

    private final String[] prefixes = {"Pro", "Noob", "xX", "The", "Epic", "Dark", "Ghost", "Ninja", "Super", "Mega", "Ultra", "Fast", "Iron", "Gold", "Shadow", "King", "Wolf"};
    private final String[] suffixes = {"Gamer", "PVP", "Slayer", "Craft", "Boy", "Girl", "HD", "YT", "MC", "King", "Beast", "Master", "Lord", "Knight", "Hero", "Mine"};

    public GuiListener(NeDisguise plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = ChatColor.stripColor(event.getView().getTitle());
        if (title == null) return;

        String setupTitle = ChatColor.stripColor(plugin.color(plugin.getConfigManager().getSetupGuiConfig().getString("title", "Disguise: Setup")));
        String rankTitle = ChatColor.stripColor(plugin.color(plugin.getConfigManager().getRankGuiConfig().getString("title", "Disguise: Rank")));
        String nameTitle = ChatColor.stripColor(plugin.color(plugin.getConfigManager().getNameGuiConfig().getString("title", "Disguise: Name")));
        String skinTitle = ChatColor.stripColor(plugin.color(plugin.getConfigManager().getSkinGuiConfig().getString("title", "Disguise: Skin")));

        boolean isGui = title.equals(setupTitle) || title.equals(rankTitle) || title.equals(nameTitle) || title.equals(skinTitle);
        if (!isGui) return;

        event.setCancelled(true);

        if (event.getRawSlot() >= event.getInventory().getSize() || event.getRawSlot() < 0) {
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        int slot = event.getRawSlot();

        if (title.equals(setupTitle)) {
            handleSetupClick(player, slot, item);
        } else if (title.equals(rankTitle)) {
            handleRankClick(player, slot, item);
        } else if (title.equals(nameTitle)) {
            handleNameClick(player, slot, item);
        } else if (title.equals(skinTitle)) {
            handleSkinClick(player, slot, item);
        }
    }

    private void playSoundSafe(Player player, String soundName) {
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (Exception ignored) {}
    }

    private void handleSetupClick(Player player, int slot, ItemStack item) {
        FileConfiguration config = plugin.getConfigManager().getSetupGuiConfig();

        int closeSlot = config.getInt("items.close.slot", 45);
        int rankSlot = config.getInt("items.rank_menu.slot", 48);
        int nameSlot = config.getInt("items.name_menu.slot", 49);
        int skinSlot = config.getInt("items.skin_menu.slot", 50);
        int applySlot = config.getInt("items.status.slot", 31);

        if (slot == closeSlot) {
            player.closeInventory();
            playSoundSafe(player, plugin.getConfig().getString("sounds.cancel", "NOTE_BASS"));
        } else if (slot == rankSlot) {
            playSoundSafe(player, plugin.getConfig().getString("sounds.click", "CLICK"));
            plugin.getGuiManager().openRankGui(player);
        } else if (slot == nameSlot) {
            playSoundSafe(player, plugin.getConfig().getString("sounds.click", "CLICK"));
            plugin.getGuiManager().openNameGui(player);
        } else if (slot == skinSlot) {
            playSoundSafe(player, plugin.getConfig().getString("sounds.click", "CLICK"));
            plugin.getGuiManager().openSkinGui(player);
        } else if (slot == applySlot) {
            // Apply Disguise
            DisguiseData data = plugin.getGuiManager().sessionData.get(player.getUniqueId());
            if (data == null) {
                data = new DisguiseData(player.getName(), null, null, null);
                plugin.getGuiManager().sessionData.put(player.getUniqueId(), data);
            }

            // If rank not selected, pick default
            if (data.getRank() == null) {
                List<String> groups = plugin.getConfigManager().getRankGuiConfig().getStringList("ranks.groups");
                if (!groups.isEmpty()) {
                    data.setRank(groups.get(0));
                } else {
                    data.setRank("default");
                }
            }

            // If name not selected, pick random
            if (data.getName() == null) {
                String prefix = prefixes[random.nextInt(prefixes.length)];
                String suffix = suffixes[random.nextInt(suffixes.length)];
                data.setName(prefix + suffix + random.nextInt(99));
            }

            // If skin not selected, use name
            if (data.getSkin() == null) {
                data.setSkin(data.getName());
            }

            data.setRealName(player.getName());
            plugin.getDatabaseManager().saveDisguiseData(player.getUniqueId(), data);

            PlayerListener listener = new PlayerListener(plugin);
            listener.changeName(player, data.getName(), data.getRank());

            player.closeInventory();
            player.sendMessage(plugin.color(plugin.getConfig().getString("messages.disguised")
                    .replace("{name}", data.getName())
                    .replace("{rank}", data.getRank())));
            playSoundSafe(player, plugin.getConfig().getString("sounds.success", "LEVEL_UP"));
        }
    }

    private void handleRankClick(Player player, int slot, ItemStack item) {
        FileConfiguration config = plugin.getConfigManager().getRankGuiConfig();

        int backSlot = config.getInt("items.back.slot", 45);
        int unnickSlot = config.getInt("items.unnick.slot", 53);

        if (slot == backSlot) {
            playSoundSafe(player, plugin.getConfig().getString("sounds.click", "CLICK"));
            plugin.getGuiManager().openSetupGui(player);
            return;
        }

        if (slot == unnickSlot) {
            player.closeInventory();
            playSoundSafe(player, plugin.getConfig().getString("sounds.success", "LEVEL_UP"));
            plugin.getGuiManager().sessionData.remove(player.getUniqueId());

            String originalName = player.getName();
            try {
                DisguiseData dbData = plugin.getDatabaseManager().getDisguiseData(player.getUniqueId()).get();
                if (dbData != null && dbData.getRealName() != null) {
                    originalName = dbData.getRealName();
                }
            } catch (Exception ignored) {}

            plugin.getDatabaseManager().removeDisguiseData(player.getUniqueId());

            PlayerListener listener = new PlayerListener(plugin);
            listener.changeName(player, originalName, "");
            player.sendMessage(plugin.color(plugin.getConfig().getString("messages.undisguised")));
            return;
        }

        List<Integer> rankSlots = config.getIntegerList("ranks.slots");
        List<String> groups = config.getStringList("ranks.groups");

        for (int i = 0; i < rankSlots.size() && i < groups.size(); i++) {
            if (slot == rankSlots.get(i)) {
                String selectedGroup = groups.get(i);
                DisguiseData data = plugin.getGuiManager().sessionData.computeIfAbsent(player.getUniqueId(), k -> new DisguiseData(player.getName(), null, null, null));
                data.setRank(selectedGroup);
                playSoundSafe(player, plugin.getConfig().getString("sounds.click", "CLICK"));
                plugin.getGuiManager().openSetupGui(player);
                return;
            }
        }
    }

    private void handleNameClick(Player player, int slot, ItemStack item) {
        FileConfiguration config = plugin.getConfigManager().getNameGuiConfig();

        int backSlot = config.getInt("items.back.slot", 45);
        int customSlot = config.getInt("items.custom_name.slot", 30);
        int randomSlot = config.getInt("items.random_name.slot", 32);

        if (slot == backSlot) {
            playSoundSafe(player, plugin.getConfig().getString("sounds.click", "CLICK"));
            plugin.getGuiManager().openSetupGui(player);
            return;
        }

        if (slot == randomSlot) {
            String prefix = prefixes[random.nextInt(prefixes.length)];
            String suffix = suffixes[random.nextInt(suffixes.length)];
            String randName = prefix + suffix + random.nextInt(99);

            DisguiseData data = plugin.getGuiManager().sessionData.computeIfAbsent(player.getUniqueId(), k -> new DisguiseData(player.getName(), null, null, null));
            data.setName(randName);

            playSoundSafe(player, plugin.getConfig().getString("sounds.click", "CLICK"));
            plugin.getGuiManager().openNameGui(player);
            return;
        }

        if (slot == customSlot) {
            player.closeInventory();
            playSoundSafe(player, plugin.getConfig().getString("sounds.click", "CLICK"));

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

                        DisguiseData data = plugin.getGuiManager().sessionData.computeIfAbsent(p.getUniqueId(), k -> new DisguiseData(p.getName(), null, null, null));
                        data.setName(nameInput);

                        return Arrays.asList(SignGUIAction.run(() -> {
                            Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openSetupGui(p));
                        }));
                    })
                    .build()
                    .open(player);
        }
    }

    private void handleSkinClick(Player player, int slot, ItemStack item) {
        FileConfiguration config = plugin.getConfigManager().getSkinGuiConfig();

        int backSlot = config.getInt("items.back.slot", 45);
        int randomSlot = config.getInt("items.random_skin.slot", 31);

        if (slot == backSlot) {
            playSoundSafe(player, plugin.getConfig().getString("sounds.click", "CLICK"));
            plugin.getGuiManager().openSetupGui(player);
            return;
        }

        if (slot == randomSlot) {
            String[] skins = {"Notch", "Dinnerbone", "Dream", "Technoblade", "TommyInnit", "Grian", "MumboJumbo", "DanTDM", "Steve", "Alex"};
            String randSkin = skins[random.nextInt(skins.length)];

            DisguiseData data = plugin.getGuiManager().sessionData.computeIfAbsent(player.getUniqueId(), k -> new DisguiseData(player.getName(), null, null, null));
            data.setSkin(randSkin);

            playSoundSafe(player, plugin.getConfig().getString("sounds.click", "CLICK"));
            plugin.getGuiManager().openSkinGui(player);
        }
    }
}
