package me.nedayazady;

import me.nedayazady.commands.AdminCommand;
import me.nedayazady.commands.RedisguiseCommand;
import me.nedayazady.commands.UndisguiseCommand;
import me.nedayazady.commands.UserCommand;
import me.nedayazady.database.DatabaseManager;
import me.nedayazady.gui.GuiManager;
import me.nedayazady.listeners.CommandListener;
import me.nedayazady.listeners.GuiListener;
import me.nedayazady.listeners.PlayerListener;
import me.nedayazady.managers.ConfigManager;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class NeDisguise extends JavaPlugin implements CommandExecutor {

    private static NeDisguise instance;
    private LuckPerms luckPerms;
    private DatabaseManager databaseManager;
    private ConfigManager configManager;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Hook LuckPerms
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPerms = provider.getProvider();
        } else {
            getLogger().severe("LuckPerms not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        configManager = new ConfigManager(this);
        databaseManager = new DatabaseManager(this);
        guiManager = new GuiManager(this);

        // Register Listeners
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Register Commands
        getCommand("disguise").setExecutor(this);
        getCommand("undisguise").setExecutor(new UndisguiseCommand(this));
        getCommand("redisguise").setExecutor(new RedisguiseCommand(this));
        getCommand("user").setExecutor(new UserCommand(this));
        getCommand("disguiseadmin").setExecutor(new AdminCommand(this));

        getLogger().info("NeDisguise has been enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("NeDisguise has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        
        if (!player.hasPermission("core.command.disguise")) {
            player.sendMessage(color(getConfig().getString("messages.no_permission")));
            return true;
        }

        guiManager.openSetupGui(player);
        return true;
    }

    public static NeDisguise getInstance() {
        return instance;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
