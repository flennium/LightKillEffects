package org.flennn.lightkilleffects.config;

import org.bukkit.configuration.file.FileConfiguration;

public final class PluginSettings {
    private final FileConfiguration config;

    public PluginSettings(FileConfiguration config) {
        this.config = config;
    }

    public boolean isEnabled() {
        return this.config.getBoolean("general.enabled", true);
    }

    public boolean isDebug() {
        return this.config.getBoolean("general.debug", false);
    }

    public boolean allowConsoleSetOthers() {
        return this.config.getBoolean("security.allow-console-set-others", true);
    }

    public boolean requirePreviewUnlock() {
        return this.config.getBoolean("security.require-preview-unlock", true);
    }

    public boolean allowMobKillEffects() {
        return this.config.getBoolean("security.allow-mob-kill-effects", true);
    }

    public int maxBackups() {
        return clamp(this.config.getInt("storage.max-backups", 10), 1, 100);
    }

    public boolean backupBeforeImport() {
        return this.config.getBoolean("storage.backup-before-import", true);
    }

    public boolean restrictImportsToDataFolder() {
        return this.config.getBoolean("storage.restrict-imports-to-data-folder", true);
    }

    public int guiSize() {
        int size = this.config.getInt("gui.size", 54);
        if (size % 9 != 0) {
            return 54;
        }
        return clamp(size, 9, 54);
    }

    public int maxFavorites() {
        return Math.max(0, this.config.getInt("general.max-favorites", 5));
    }

    public int particleRenderDistance() {
        return clamp(this.config.getInt("performance.particle-render-distance", 32), 1, 128);
    }

    public int maxParticlesPerEffect() {
        return clamp(this.config.getInt("performance.max-particles-per-effect", 350), 10, 2000);
    }

    public int cleanupIntervalTicks() {
        return clamp(this.config.getInt("performance.cleanup-interval", 200), 20, 72000);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
