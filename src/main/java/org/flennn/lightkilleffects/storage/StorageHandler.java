package org.flennn.lightkilleffects.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.flennn.lightkilleffects.LightKillEffects;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Handles data storage and persistence for player data and plugin configuration
 */
public class StorageHandler {
    
    private final LightKillEffects plugin;
    private File playerDataFile;
    private FileConfiguration playerDataConfig;
    
    public StorageHandler(LightKillEffects plugin) {
        this.plugin = plugin;
        setupDataFiles();
    }
    
    /**
     * Setup and create necessary data files
     */
    private void setupDataFiles() {
        // Ensure data folder exists
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().severe("Could not create plugin data folder.");
        }
        
        // Setup player data file
        playerDataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!playerDataFile.exists()) {
            plugin.saveResource("playerdata.yml", false);
        }
        
        playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
        
        plugin.logDebug("Data files setup completed");
    }
    
    /**
     * Save player data to file
     */
    public void savePlayerData() {
        try {
            playerDataConfig.save(playerDataFile);
            plugin.logDebug("Player data saved successfully");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Reload player data from file
     */
    public void reloadPlayerData() {
        try {
            playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
            plugin.logDebug("Player data reloaded successfully");
        } catch (Exception e) {
            plugin.getLogger().severe("Could not reload player data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Save all data (called on disable)
     */
    public void saveAllData() {
        savePlayerData();
        plugin.logDebug("All data saved");
    }
    
    /**
     * Create backup of player data
     */
    public boolean createBackup() {
        try {
            File backupDir = new File(plugin.getDataFolder(), "backups");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            
            String timestamp = String.valueOf(System.currentTimeMillis());
            File backupFile = new File(backupDir, "playerdata_" + timestamp + ".yml");
            
            Files.copy(playerDataFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.logInfo("&aBackup created: " + backupFile.getName());
            
            // Clean old backups (keep only last 10)
            cleanOldBackups(backupDir);
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create backup: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Clean old backup files, keeping only the most recent ones
     */
    private void cleanOldBackups(File backupDir) {
        File[] backupFiles = backupDir.listFiles((dir, name) -> name.startsWith("playerdata_") && name.endsWith(".yml"));
        int maxBackups = plugin.getSettings().maxBackups();
        if (backupFiles == null || backupFiles.length <= maxBackups) {
            return;
        }
        
        // Sort by modification time (newest first)
        java.util.Arrays.sort(backupFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        
        // Delete files beyond the 10 most recent
        for (int i = maxBackups; i < backupFiles.length; i++) {
            if (backupFiles[i].delete()) {
                plugin.logDebug("Deleted old backup: " + backupFiles[i].getName());
            }
        }
    }
    
    /**
     * Import player data from another file
     */
    public boolean importPlayerData(File importFile) {
        try {
            if (!isSafeDataFile(importFile) || !importFile.exists() || !importFile.isFile()) {
                plugin.logInfo("&cImport file does not exist: " + importFile.getName());
                return false;
            }
            
            FileConfiguration importConfig = YamlConfiguration.loadConfiguration(importFile);
            
            // Create backup before importing
            if (plugin.getSettings().backupBeforeImport()) {
                createBackup();
            }
            
            // Import data
            for (String key : importConfig.getKeys(true)) {
                playerDataConfig.set(key, importConfig.get(key));
            }
            
            savePlayerData();
            plugin.logInfo("&aPlayer data imported successfully from: " + importFile.getName());
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to import player data: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Export player data to a file
     */
    public boolean exportPlayerData(File exportFile) {
        try {
            if (!isSafeDataFile(exportFile)) {
                plugin.logInfo("&cExport path is not allowed: " + exportFile.getName());
                return false;
            }

            File parent = exportFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.logInfo("&cCould not create export folder: " + parent.getName());
                return false;
            }

            FileConfiguration exportConfig = new YamlConfiguration();
            
            // Copy all player data
            for (String key : playerDataConfig.getKeys(true)) {
                exportConfig.set(key, playerDataConfig.get(key));
            }
            
            exportConfig.save(exportFile);
            plugin.logInfo("&aPlayer data exported successfully to: " + exportFile.getName());
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to export player data: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Check if storage is healthy
     */
    public boolean isHealthy() {
        return playerDataFile.exists() && playerDataFile.canRead() && playerDataFile.canWrite();
    }
    
    /**
     * Get storage statistics
     */
    public StorageStats getStorageStats() {
        return new StorageStats(
                playerDataFile.length(),
                playerDataConfig.getKeys(false).size(),
                playerDataFile.lastModified()
        );
    }
    
    /**
     * Reset all player data (dangerous operation)
     */
    public boolean resetAllPlayerData() {
        try {
            // Create backup before reset
            createBackup();
            
            // Clear all data
            for (String key : playerDataConfig.getKeys(false)) {
                playerDataConfig.set(key, null);
            }
            
            savePlayerData();
            plugin.logInfo("&cAll player data has been reset!");
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to reset player data: " + e.getMessage());
            e.printStackTrace();
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

        Path dataFolder = plugin.getDataFolder().getCanonicalFile().toPath();
        Path target = file.getCanonicalFile().toPath();
        return target.startsWith(dataFolder);
    }
    
    // Getters
    public FileConfiguration getPlayerDataConfig() {
        return playerDataConfig;
    }
    
    public File getPlayerDataFile() {
        return playerDataFile;
    }
    
    /**
     * Data class for storage statistics
     */
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
