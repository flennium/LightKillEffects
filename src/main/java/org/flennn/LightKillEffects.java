package org.flennn;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for LightKillEffects
 * Manages stunning light-based kill effects with 20 unique visual effects
 */
public class LightKillEffects extends JavaPlugin {
    
    private static LightKillEffects instance;
    private KillEffectManager effectManager;
    private StorageHandler storageHandler;
    private PlayerData playerData;
    private EffectMenu effectMenu;
    private boolean debugMode;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Load configuration
        saveDefaultConfig();
        loadConfiguration();
        
        // Initialize core components
        initializeComponents();
        
        // Register events and commands
        registerEvents();
        registerCommands();
        
        // Log successful startup
        logInfo("&aLightKillEffects v" + getDescription().getVersion() + " has been enabled!");
        logInfo("&a20 stunning light-based kill effects are now available!");
        
        // Performance check
        if (getConfig().getBoolean("performance.performance-mode", false)) {
            logInfo("&ePerformance mode is enabled - reduced particle effects for better performance");
        }
    }
    
    @Override
    public void onDisable() {
        // Cancel all running effect tasks
        if (effectManager != null) {
            effectManager.cleanup();
        }
        
        // Save all player data
        if (storageHandler != null) {
            storageHandler.saveAllData();
        }
        
        // Clear temporary blocks
        if (effectManager != null) {
            effectManager.clearAllTemporaryBlocks();
        }
        
        logInfo("&cLightKillEffects has been disabled. All effects cleaned up.");
        instance = null;
    }
    
    /**
     * Load and validate configuration
     */
    private void loadConfiguration() {
        reloadConfig();
        
        // Validate configuration values
        validateConfigValues();
        
        // Set debug mode
        debugMode = getConfig().getBoolean("general.debug", false);
        
        if (debugMode) {
            logInfo("&eDebug mode is enabled");
        }
    }
    
    /**
     * Validate configuration values and set defaults if needed
     */
    private void validateConfigValues() {
        boolean configChanged = false;
        
        // Validate general settings
        if (getConfig().getInt("general.global-cooldown") < 1) {
            getConfig().set("general.global-cooldown", 3);
            configChanged = true;
        }
        
        if (getConfig().getInt("general.max-favorites") < 1) {
            getConfig().set("general.max-favorites", 5);
            configChanged = true;
        }
        
        // Validate performance settings
        if (getConfig().getInt("performance.max-particles-per-effect") < 10) {
            getConfig().set("performance.max-particles-per-effect", 500);
            configChanged = true;
        }
        
        if (getConfig().getInt("performance.particle-render-distance") < 5) {
            getConfig().set("performance.particle-render-distance", 32);
            configChanged = true;
        }
        
        // Validate GUI settings
        int guiSize = getConfig().getInt("gui.size", 54);
        if (guiSize % 9 != 0 || guiSize < 9 || guiSize > 54) {
            getConfig().set("gui.size", 54);
            configChanged = true;
        }
        
        // Save config if changes were made
        if (configChanged) {
            saveConfig();
            logInfo("&eConfiguration values were corrected and saved");
        }
    }
    
    /**
     * Initialize all core components
     */
    private void initializeComponents() {
        try {
            // Initialize storage handler
            storageHandler = new StorageHandler(this);
            logDebug("StorageHandler initialized");
            
            // Initialize player data manager
            playerData = new PlayerData(this, storageHandler);
            logDebug("PlayerData initialized");
            
            // Initialize effect manager
            effectManager = new KillEffectManager(this);
            logDebug("KillEffectManager initialized");
            
            // Initialize GUI system
            effectMenu = new EffectMenu(this);
            logDebug("EffectMenu initialized");
            
            // Register EffectMenu event listener
            getServer().getPluginManager().registerEvents(effectMenu, this);
            logDebug("EffectMenu events registered");
            
            logInfo("&aAll core components initialized successfully");
            
        } catch (Exception e) {
            getLogger().severe("Failed to initialize core components: " + e.getMessage());
            e.printStackTrace();
            setEnabled(false);
        }
    }
    
    /**
     * Register event listeners
     */
    private void registerEvents() {
        try {
            // Register kill listener
            getServer().getPluginManager().registerEvents(new KillListener(this), this);
            logDebug("KillListener registered");
            
            logInfo("&aEvent listeners registered successfully");
            
        } catch (Exception e) {
            getLogger().severe("Failed to register event listeners: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Register commands
     */
    private void registerCommands() {
        try {
            // Register main command
            KillEffectCommand commandExecutor = new KillEffectCommand(this);
            getCommand("killeffects").setExecutor(commandExecutor);
            getCommand("killeffects").setTabCompleter(commandExecutor);
            logDebug("Commands registered");
            
            logInfo("&aCommands registered successfully");
            
        } catch (Exception e) {
            getLogger().severe("Failed to register commands: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Reload the plugin configuration and components
     */
    public void reloadPlugin() {
        try {
            // Save current data
            if (storageHandler != null) {
                storageHandler.saveAllData();
            }
            
            // Cancel running effects
            if (effectManager != null) {
                effectManager.cleanup();
            }
            
            // Reload configuration
            loadConfiguration();
            
            // Reinitialize components
            initializeComponents();
            
            logInfo("&aPlugin reloaded successfully!");
            
        } catch (Exception e) {
            getLogger().severe("Failed to reload plugin: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Check if the plugin is enabled and ready
     */
    public boolean isReady() {
        return isEnabled() && effectManager != null && storageHandler != null && playerData != null && effectMenu != null;
    }
    
    /**
     * Get the default effect for new players
     */
    public EffectType getDefaultEffect() {
        String defaultEffectKey = getConfig().getString("general.default-effect", "prismatic_shatter");
        if ("none".equalsIgnoreCase(defaultEffectKey)) {
            return null;
        }
        return EffectType.fromConfigKey(defaultEffectKey);
    }
    
    /**
     * Utility method to log colored messages
     */
    public void logInfo(String message) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&8⟜ &6⚡ LOG &8⟝ " + message));
    }
    
    /**
     * Utility method to log debug messages
     */
    public void logDebug(String message) {
        if (debugMode) {
            logInfo("&7[DEBUG] " + message);
        }
    }
    
    /**
     * Get formatted message from config
     */
    public String getMessage(String key) {
        String prefix = getConfig().getString("messages.prefix", "&8⟜ &6⚡ &8⟝ ");
        String message = getConfig().getString("messages." + key, "&cMessage not found: " + key);
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }
    
    /**
     * Get formatted message from config with placeholder replacement
     */
    public String getMessage(String key, String placeholder, String value) {
        String prefix = getConfig().getString("messages.prefix", "&8⟜ &6⚡ &8⟝ ");
        String message = getConfig().getString("messages." + key, "&cMessage not found: " + key);
        
        // Replace placeholder
        message = message.replace("{" + placeholder + "}", value);
        
        // Translate color codes for the entire message (prefix + message with replaced placeholders)
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }
    
    // Getters
    public static LightKillEffects getInstance() {
        return instance;
    }
    
    public KillEffectManager getEffectManager() {
        return effectManager;
    }
    
    public StorageHandler getStorageHandler() {
        return storageHandler;
    }
    
    public PlayerData getPlayerData() {
        return playerData;
    }
    
    public EffectMenu getEffectMenu() {
        return effectMenu;
    }
    
    public boolean isDebugMode() {
        return debugMode;
    }
}
