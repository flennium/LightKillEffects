package org.flennn.lightkilleffects;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.flennn.lightkilleffects.command.KillEffectCommand;
import org.flennn.lightkilleffects.config.PluginSettings;
import org.flennn.lightkilleffects.effect.EffectType;
import org.flennn.lightkilleffects.effect.KillEffectManager;
import org.flennn.lightkilleffects.feedback.FeedbackManager;
import org.flennn.lightkilleffects.listener.KillListener;
import org.flennn.lightkilleffects.menu.EffectMenu;
import org.flennn.lightkilleffects.permission.PermissionManager;
import org.flennn.lightkilleffects.preview.PreviewEnvironmentManager;
import org.flennn.lightkilleffects.preview.PreviewListener;
import org.flennn.lightkilleffects.storage.PlayerData;
import org.flennn.lightkilleffects.storage.StorageHandler;
import org.flennn.lightkilleffects.util.Console;

import java.io.File;

public class LightKillEffects extends JavaPlugin {
    private static LightKillEffects instance;

    private KillEffectManager effectManager;
    private StorageHandler storageHandler;
    private PlayerData playerData;
    private EffectMenu effectMenu;
    private PluginSettings settings;
    private FeedbackManager feedbackManager;
    private PreviewEnvironmentManager previewManager;
    private PermissionManager permissionManager;
    private FileConfiguration categoriesConfig;
    private FileConfiguration effectsConfig;
    private FileConfiguration messagesConfig;
    private boolean debugMode;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadConfiguration();

        if (!this.settings.isEnabled()) {
            Console.warn("Plugin is disabled in config.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        initializeComponents();
        registerEvents();
        registerCommands();

        Console.success("LightKillEffects v" + getDescription().getVersion() + " enabled.");
        Console.info(EffectType.values().length + " kill effects loaded.");

        if (getConfig().getBoolean("performance.performance-mode", true)) {
            Console.warn("Performance mode is enabled.");
        }
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);

        if (this.effectManager != null) {
            this.effectManager.cleanup();
            this.effectManager.clearAllTemporaryBlocks();
        }

        if (this.previewManager != null) {
            this.previewManager.cleanup();
        }

        if (this.storageHandler != null) {
            this.storageHandler.saveAllData();
        }

        Console.info("LightKillEffects disabled. Effects cleaned up.");
        instance = null;
    }

    private void loadConfiguration() {
        reloadConfig();
        saveExtraConfig("categories.yml");
        saveExtraConfig("effects.yml");
        saveExtraConfig("messages.yml");
        this.categoriesConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "categories.yml"));
        this.effectsConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "effects.yml"));
        this.messagesConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));
        this.settings = new PluginSettings(getConfig());
        this.feedbackManager = new FeedbackManager(this);
        validateConfigValues();
        this.debugMode = this.settings.isDebug();

        if (this.debugMode) {
            Console.warn("Debug mode is enabled.");
        }
    }

    private void validateConfigValues() {
        boolean changed = false;
        changed |= setIfBelow("general.global-cooldown", 0, 3);
        changed |= setIfBelow("general.max-favorites", 0, 5);
        changed |= setIfBelow("performance.max-particles-per-effect", 10, 350);
        changed |= setIfBelow("performance.particle-render-distance", 1, 32);
        changed |= setIfBelow("performance.cleanup-interval", 20, 200);
        changed |= setIfBelow("storage.max-backups", 1, 10);
        changed |= setIfBelow("feedback-cooldowns.chat.default-ms", 0, 1200);
        changed |= setIfBelow("feedback-cooldowns.ui.default-ms", 0, 250);

        int guiSize = getConfig().getInt("gui.size", 54);
        if (guiSize % 9 != 0 || guiSize < 9 || guiSize > 54) {
            getConfig().set("gui.size", 54);
            changed = true;
        }

        if (changed) {
            saveConfig();
            Console.warn("Invalid config values were corrected and saved.");
            this.settings = new PluginSettings(getConfig());
        }
    }

    private boolean setIfBelow(String path, int min, int replacement) {
        if (getConfig().getInt(path, replacement) >= min) {
            return false;
        }
        getConfig().set(path, replacement);
        return true;
    }

    private void initializeComponents() {
        try {
            this.storageHandler = new StorageHandler(this);
            this.playerData = new PlayerData(this, this.storageHandler);
            this.effectManager = new KillEffectManager(this);
            this.effectMenu = new EffectMenu(this);
            this.previewManager = new PreviewEnvironmentManager(this);
            this.permissionManager = new PermissionManager(this);
            Console.success("Core components initialized.");
        } catch (Exception e) {
            Console.error("Failed to initialize core components: " + e.getMessage());
            getLogger().severe(e.toString());
            setEnabled(false);
        }
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(this.effectMenu, this);
        getServer().getPluginManager().registerEvents(new KillListener(this), this);
        getServer().getPluginManager().registerEvents(new PreviewListener(this, this.previewManager), this);
        logDebug("Event listeners registered");
    }

    private void registerCommands() {
        PluginCommand command = getCommand("killeffects");
        if (command == null) {
            Console.error("Command 'killeffects' is missing from plugin.yml.");
            return;
        }

        KillEffectCommand commandExecutor = new KillEffectCommand(this);
        command.setExecutor(commandExecutor);
        command.setTabCompleter(commandExecutor);
        logDebug("Commands registered");
    }

    public void reloadPlugin() {
        try {
            if (this.storageHandler != null) {
                this.storageHandler.saveAllData();
            }
            if (this.effectManager != null) {
                this.effectManager.cleanup();
                this.effectManager.clearAllTemporaryBlocks();
            }

            HandlerList.unregisterAll(this);
            loadConfiguration();
            initializeComponents();
            registerEvents();
            Console.success("Plugin reloaded.");
        } catch (Exception e) {
            Console.error("Failed to reload plugin: " + e.getMessage());
            getLogger().severe(e.toString());
        }
    }

    public boolean isReady() {
        return isEnabled() && this.effectManager != null && this.storageHandler != null && this.playerData != null && this.effectMenu != null;
    }

    public EffectType getDefaultEffect() {
        String defaultEffectKey = getConfig().getString("general.default-effect", "none");
        if ("none".equalsIgnoreCase(defaultEffectKey)) {
            return null;
        }
        return EffectType.fromConfigKey(defaultEffectKey);
    }

    public void logInfo(String message) {
        Console.info(message);
    }

    public void logDebug(String message) {
        if (this.debugMode) {
            Console.info("&7[DEBUG] " + message);
        }
    }

    public void logWarn(String message) {
        Console.warn(message);
    }

    public String getMessage(String key) {
        String prefix = this.messagesConfig.getString("messages.prefix", "&8[&eLKE&8] ");
        String message = this.messagesConfig.getString("messages." + key, "&cMessage not found: " + key);
        return Console.color(prefix + message);
    }

    public boolean sendMessage(org.bukkit.command.CommandSender sender, String key) {
        return this.feedbackManager.send(sender, key, getMessage(key));
    }

    public boolean sendMessage(org.bukkit.command.CommandSender sender, String key, String placeholder, String value) {
        return this.feedbackManager.send(sender, key, getMessage(key, placeholder, value));
    }

    public boolean sendMessage(org.bukkit.command.CommandSender sender, String key, String... replacements) {
        String message = getMessage(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return this.feedbackManager.send(sender, key, message);
    }

    public boolean sendRawMessage(org.bukkit.command.CommandSender sender, String key, String message) {
        return this.feedbackManager.send(sender, key, Console.color(message));
    }

    public boolean playUiFeedback(org.bukkit.entity.Player player, String key, org.bukkit.Sound sound, float volume, float pitch) {
        return this.feedbackManager.playUi(player, key, sound, volume, pitch);
    }

    public FileConfiguration getCategoriesConfig() {
        return this.categoriesConfig;
    }

    public FileConfiguration getEffectsConfig() {
        return this.effectsConfig;
    }

    public FileConfiguration getMessagesConfig() {
        return this.messagesConfig;
    }

    private void saveExtraConfig(String name) {
        File file = new File(getDataFolder(), name);
        if (!file.exists()) {
            saveResource(name, false);
        }
    }

    public String getMessage(String key, String placeholder, String value) {
        String prefix = this.messagesConfig.getString("messages.prefix", "&8[&eLKE&8] ");
        String message = this.messagesConfig.getString("messages." + key, "&cMessage not found: " + key);
        message = message.replace("{" + placeholder + "}", value);
        return Console.color(prefix + message);
    }

    public static LightKillEffects getInstance() {
        return instance;
    }

    public KillEffectManager getEffectManager() {
        return this.effectManager;
    }

    public StorageHandler getStorageHandler() {
        return this.storageHandler;
    }

    public PlayerData getPlayerData() {
        return this.playerData;
    }

    public EffectMenu getEffectMenu() {
        return this.effectMenu;
    }

    public PluginSettings getSettings() {
        return this.settings;
    }

    public FeedbackManager getFeedbackManager() {
        return this.feedbackManager;
    }

    public boolean isDebugMode() {
        return this.debugMode;
    }

    public PreviewEnvironmentManager getPreviewManager() {
        return this.previewManager;
    }

    public PermissionManager getPermissionManager() {
        return this.permissionManager;
    }
}
