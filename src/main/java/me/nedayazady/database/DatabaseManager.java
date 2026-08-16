package me.nedayazady.database;

import me.nedayazady.NeDisguise;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final NeDisguise plugin;
    private HikariDataSource dataSource;
    private boolean useMysql;
    private File yamlFile;
    private FileConfiguration yamlConfig;

    public DatabaseManager(NeDisguise plugin) {
        this.plugin = plugin;
        setupDatabase();
    }

    private void setupDatabase() {
        this.useMysql = plugin.getConfig().getBoolean("mysql.enabled");

        if (useMysql) {
            String host = plugin.getConfig().getString("mysql.host");
            int port = plugin.getConfig().getInt("mysql.port");
            String database = plugin.getConfig().getString("mysql.database");
            String username = plugin.getConfig().getString("mysql.username");
            String password = plugin.getConfig().getString("mysql.password");

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true");
            config.setUsername(username);
            config.setPassword(password);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
            createTable();
        } else {
            yamlFile = new File(plugin.getDataFolder(), "data.yml");
            if (!yamlFile.exists()) {
                try {
                    yamlFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            yamlConfig = YamlConfiguration.loadConfiguration(yamlFile);
        }
    }

    private void createTable() {
        if (!useMysql) return;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS disguise_data (" +
                             "uuid VARCHAR(36) PRIMARY KEY," +
                             "real_name VARCHAR(16)," +
                             "disguise_name VARCHAR(16)," +
                             "rank_group VARCHAR(32)," +
                             "skin_texture TEXT" +
                             ")")) {
            ps.execute();
            
            // Try to add the column if it doesn't exist (for existing tables)
            try (PreparedStatement alter = connection.prepareStatement(
                    "ALTER TABLE disguise_data ADD COLUMN real_name VARCHAR(16) AFTER uuid")) {
                alter.execute();
            } catch (SQLException ignored) {}
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public CompletableFuture<DisguiseData> getDisguiseData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (useMysql) {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement ps = connection.prepareStatement("SELECT * FROM disguise_data WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            // Support for older rows that might not have real_name set yet
                            String realName = null;
                            try {
                                realName = rs.getString("real_name");
                            } catch (SQLException ignored) {}
                            
                            return new DisguiseData(
                                    realName,
                                    rs.getString("disguise_name"),
                                    rs.getString("rank_group"),
                                    rs.getString("skin_texture")
                            );
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                String path = uuid.toString();
                if (yamlConfig.contains(path)) {
                    return new DisguiseData(
                            yamlConfig.getString(path + ".realName"),
                            yamlConfig.getString(path + ".name"),
                            yamlConfig.getString(path + ".rank"),
                            yamlConfig.getString(path + ".skin")
                    );
                }
            }
            return null;
        });
    }

    public void saveDisguiseData(UUID uuid, DisguiseData data) {
        CompletableFuture.runAsync(() -> {
            if (useMysql) {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement ps = connection.prepareStatement(
                             "REPLACE INTO disguise_data (uuid, real_name, disguise_name, rank_group, skin_texture) VALUES (?, ?, ?, ?, ?)")) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, data.getRealName());
                    ps.setString(3, data.getName());
                    ps.setString(4, data.getRank());
                    ps.setString(5, data.getSkin());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                String path = uuid.toString();
                yamlConfig.set(path + ".realName", data.getRealName());
                yamlConfig.set(path + ".name", data.getName());
                yamlConfig.set(path + ".rank", data.getRank());
                yamlConfig.set(path + ".skin", data.getSkin());
                saveYaml();
            }
        });
    }
    
    public void removeDisguiseData(UUID uuid) {
        CompletableFuture.runAsync(() -> {
            if (useMysql) {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement ps = connection.prepareStatement("DELETE FROM disguise_data WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                yamlConfig.set(uuid.toString(), null);
                saveYaml();
            }
        });
    }

    private void saveYaml() {
        try {
            yamlConfig.save(yamlFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
