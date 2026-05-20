package org.flennn.lightkilleffects.storage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.flennn.lightkilleffects.LightKillEffects;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StorageHandler {
    private static final String SQLITE_KEY = "playerdata_yaml";

    private final LightKillEffects plugin;
    private final Gson gson = new Gson();
    private File dataDir;
    private File playerDataFile;
    private FileConfiguration playerDataConfig;
    private String backend;

    public StorageHandler(LightKillEffects plugin) {
        this.plugin = plugin;
        setupDataFiles();
    }

    private void setupDataFiles() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().severe("Could not create plugin data folder.");
        }

        this.dataDir = new File(plugin.getDataFolder(), "data");
        if (!this.dataDir.exists() && !this.dataDir.mkdirs()) {
            plugin.getLogger().severe("Could not create data folder.");
        }

        this.backend = plugin.getSettings().storageType();
        this.playerDataFile = new File(this.dataDir, storageFileName(this.backend));
        this.playerDataConfig = new YamlConfiguration();
        migrateLegacyYaml();
        if (migrateDataFolderYaml()) {
            plugin.logDebug("Existing YAML player data migrated to " + this.backend);
            return;
        }
        loadBackend();
        plugin.logDebug("Player data backend: " + this.backend);
    }

    private String storageFileName(String backend) {
        if ("json".equals(backend)) {
            return "playerdata.json";
        }
        if ("sqlite".equals(backend)) {
            return "playerdata.db";
        }
        return "playerdata.yml";
    }

    private void migrateLegacyYaml() {
        File oldFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!oldFile.exists() || this.playerDataFile.exists() || !"yaml".equals(this.backend)) {
            return;
        }

        try {
            Files.move(oldFile.toPath(), this.playerDataFile.toPath());
            plugin.logInfo("&aMoved old playerdata.yml into the data folder.");
        } catch (IOException e) {
            plugin.getLogger().warning("Could not move old playerdata.yml: " + e.getMessage());
        }
    }

    private boolean migrateDataFolderYaml() {
        File yamlFile = new File(this.dataDir, "playerdata.yml");
        if ("yaml".equals(this.backend) || this.playerDataFile.exists() || !yamlFile.exists()) {
            return false;
        }

        try {
            this.playerDataConfig.load(yamlFile);
            saveBackend();
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Could not migrate existing YAML player data: " + e.getMessage());
            return false;
        }
    }

    private void loadBackend() {
        try {
            if ("json".equals(this.backend)) {
                loadJson();
            } else if ("sqlite".equals(this.backend)) {
                loadSqlite();
            } else {
                loadYaml();
            }
        } catch (Exception e) {
            this.playerDataConfig = new YamlConfiguration();
            plugin.getLogger().severe("Could not load player data: " + e.getMessage());
        }
    }

    private void loadYaml() throws IOException, InvalidConfigurationException {
        if (!this.playerDataFile.exists()) {
            this.playerDataFile.createNewFile();
        }
        this.playerDataConfig.load(this.playerDataFile);
    }

    private void loadJson() throws IOException, InvalidConfigurationException {
        if (!this.playerDataFile.exists()) {
            saveJson("");
            return;
        }

        try (FileReader reader = new FileReader(this.playerDataFile, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            String yaml = root.has("yaml") ? root.get("yaml").getAsString() : "";
            this.playerDataConfig.loadFromString(yaml);
        }
    }

    private void loadSqlite() throws SQLException, InvalidConfigurationException {
        ensureSqliteTable();
        try (Connection connection = openSqlite();
             PreparedStatement statement = connection.prepareStatement("SELECT data_value FROM plugin_data WHERE data_key = ?")) {
            statement.setString(1, SQLITE_KEY);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    this.playerDataConfig.loadFromString(result.getString("data_value"));
                }
            }
        }
    }

    public void savePlayerData() {
        try {
            saveBackend();
            plugin.logDebug("Player data saved");
        } catch (Exception e) {
            plugin.getLogger().severe("Could not save player data: " + e.getMessage());
        }
    }

    private void saveBackend() throws IOException, SQLException {
        String yaml = this.playerDataConfig.saveToString();
        if ("json".equals(this.backend)) {
            saveJson(yaml);
        } else if ("sqlite".equals(this.backend)) {
            saveSqlite(yaml);
        } else {
            this.playerDataConfig.save(this.playerDataFile);
        }
    }

    private void saveJson(String yaml) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("format", "yaml");
        root.addProperty("yaml", yaml);
        try (FileWriter writer = new FileWriter(this.playerDataFile, StandardCharsets.UTF_8)) {
            this.gson.toJson(root, writer);
        }
    }

    private void saveSqlite(String yaml) throws SQLException {
        ensureSqliteTable();
        try (Connection connection = openSqlite();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO plugin_data(data_key, data_value) VALUES(?, ?) " +
                             "ON CONFLICT(data_key) DO UPDATE SET data_value = excluded.data_value")) {
            statement.setString(1, SQLITE_KEY);
            statement.setString(2, yaml);
            statement.executeUpdate();
        }
    }

    private void ensureSqliteTable() throws SQLException {
        try (Connection connection = openSqlite();
             PreparedStatement statement = connection.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS plugin_data (data_key TEXT PRIMARY KEY, data_value TEXT NOT NULL)")) {
            statement.executeUpdate();
        }
    }

    private Connection openSqlite() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + this.playerDataFile.getAbsolutePath());
    }

    public void reloadPlayerData() {
        loadBackend();
        plugin.logDebug("Player data reloaded");
    }

    public void saveAllData() {
        savePlayerData();
    }

    public boolean createBackup() {
        try {
            File backupDir = new File(this.dataDir, "backups");
            if (!backupDir.exists() && !backupDir.mkdirs()) {
                return false;
            }

            File backupFile = new File(backupDir, "playerdata_" + System.currentTimeMillis() + ".yml");
            Files.writeString(backupFile.toPath(), this.playerDataConfig.saveToString(), StandardCharsets.UTF_8);
            cleanOldBackups(backupDir);
            plugin.logInfo("&aBackup created: " + backupFile.getName());
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create backup: " + e.getMessage());
            return false;
        }
    }

    private void cleanOldBackups(File backupDir) {
        File[] backupFiles = backupDir.listFiles((dir, name) -> name.startsWith("playerdata_") && name.endsWith(".yml"));
        int maxBackups = plugin.getSettings().maxBackups();
        if (backupFiles == null || backupFiles.length <= maxBackups) {
            return;
        }

        java.util.Arrays.sort(backupFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        for (int i = maxBackups; i < backupFiles.length; i++) {
            if (backupFiles[i].delete()) {
                plugin.logDebug("Deleted old backup: " + backupFiles[i].getName());
            }
        }
    }

    public boolean importPlayerData(File importFile) {
        try {
            if (!isSafeDataFile(importFile) || !importFile.exists() || !importFile.isFile()) {
                plugin.logInfo("&cImport file is not allowed: " + safeName(importFile));
                return false;
            }

            FileConfiguration importConfig = YamlConfiguration.loadConfiguration(importFile);
            if (plugin.getSettings().backupBeforeImport()) {
                createBackup();
            }

            for (String key : importConfig.getKeys(true)) {
                this.playerDataConfig.set(key, importConfig.get(key));
            }

            savePlayerData();
            plugin.logInfo("&aPlayer data imported from: " + importFile.getName());
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to import player data: " + e.getMessage());
            return false;
        }
    }

    public boolean exportPlayerData(File exportFile) {
        try {
            if (!isSafeDataFile(exportFile)) {
                plugin.logInfo("&cExport path is not allowed: " + safeName(exportFile));
                return false;
            }

            File parent = exportFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.logInfo("&cCould not create export folder: " + parent.getName());
                return false;
            }

            Files.writeString(exportFile.toPath(), this.playerDataConfig.saveToString(), StandardCharsets.UTF_8);
            plugin.logInfo("&aPlayer data exported to: " + exportFile.getName());
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to export player data: " + e.getMessage());
            return false;
        }
    }

    public boolean isHealthy() {
        return this.playerDataFile.exists() && this.playerDataFile.canRead() && this.playerDataFile.canWrite();
    }

    public StorageStats getStorageStats() {
        return new StorageStats(
                this.playerDataFile.exists() ? this.playerDataFile.length() : 0,
                this.playerDataConfig.getKeys(false).size(),
                this.playerDataFile.exists() ? this.playerDataFile.lastModified() : 0
        );
    }

    public boolean resetAllPlayerData() {
        try {
            createBackup();
            for (String key : this.playerDataConfig.getKeys(false)) {
                this.playerDataConfig.set(key, null);
            }
            savePlayerData();
            plugin.logInfo("&cAll player data has been reset.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to reset player data: " + e.getMessage());
            return false;
        }
    }

    private boolean isSafeDataFile(File file) throws IOException {
        if (file == null || !file.getName().endsWith(".yml")) {
            return false;
        }

        if (!plugin.getSettings().restrictImportsToDataFolder()) {
            return true;
        }

        Path root = this.dataDir.getCanonicalFile().toPath();
        Path target = file.getCanonicalFile().toPath();
        return target.startsWith(root);
    }

    private String safeName(File file) {
        return file == null ? "null" : file.getName();
    }

    public FileConfiguration getPlayerDataConfig() {
        return this.playerDataConfig;
    }

    public File getPlayerDataFile() {
        return this.playerDataFile;
    }

    public static class StorageStats {
        private final long fileSize;
        private final int playerCount;
        private final long lastModified;

        public StorageStats(long fileSize, int playerCount, long lastModified) {
            this.fileSize = fileSize;
            this.playerCount = playerCount;
            this.lastModified = lastModified;
        }

        public long getFileSize() { return fileSize; }
        public int getPlayerCount() { return playerCount; }
        public long getLastModified() { return lastModified; }

        public String getFormattedFileSize() {
            if (fileSize < 1024) return fileSize + " B";
            if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }
}
