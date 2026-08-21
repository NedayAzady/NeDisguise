package me.nedayazady.managers;

import me.nedayazady.NeDisguise;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public class ConfigManager {

    private final NeDisguise plugin;
    private FileConfiguration setupGuiConfig;
    private FileConfiguration rankGuiConfig;
    private FileConfiguration nameGuiConfig;
    private FileConfiguration skinGuiConfig;

    public ConfigManager(NeDisguise plugin) {
        this.plugin = plugin;
        loadGuiConfigs();
    }

    public void loadGuiConfigs() {
        File guiFolder = new File(plugin.getDataFolder(), "guis");
        if (!guiFolder.exists()) {
            guiFolder.mkdirs();
        }

        setupGuiConfig = loadOrCreateConfig("setup-gui.yml", guiFolder);
        rankGuiConfig = loadOrCreateConfig("rank-gui.yml", guiFolder);
        nameGuiConfig = loadOrCreateConfig("name-gui.yml", guiFolder);
        skinGuiConfig = loadOrCreateConfig("skin-gui.yml", guiFolder);
    }

    private FileConfiguration loadOrCreateConfig(String name, File folder) {
        File file = new File(folder, name);
        if (!file.exists()) {
            try (InputStream in = plugin.getResource("guis/" + name)) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                } else {
                    file.createNewFile();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getSetupGuiConfig() {
        return setupGuiConfig;
    }

    public FileConfiguration getRankGuiConfig() {
        return rankGuiConfig;
    }

    public FileConfiguration getNameGuiConfig() {
        return nameGuiConfig;
    }

    public FileConfiguration getSkinGuiConfig() {
        return skinGuiConfig;
    }
}
