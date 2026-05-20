package org.flennn.lightkilleffects.feedback;

import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.flennn.lightkilleffects.LightKillEffects;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FeedbackManager {
    private final LightKillEffects plugin;
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public FeedbackManager(LightKillEffects plugin) {
        this.plugin = plugin;
    }

    public boolean send(CommandSender sender, String messageKey, String message) {
        if (sender == null || message == null || message.isEmpty()) {
            return false;
        }

        if (!canSend(sender, "chat", messageKey)) {
            return false;
        }

        sender.sendMessage(message);
        return true;
    }

    public boolean playUi(Player player, String actionKey, Sound sound, float volume, float pitch) {
        if (player == null || sound == null) {
            return false;
        }

        if (!tryCooldown(player, "ui", actionKey)) {
            return false;
        }

        player.playSound(player.getLocation(), sound, volume, pitch);
        return true;
    }

    public boolean tryCooldown(Player player, String channel, String key) {
        return canSend(player, channel, key);
    }

    public void clear(UUID playerId) {
        if (playerId == null) {
            return;
        }
        String prefix = playerId + ":";
        cooldowns.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public void clearAll() {
        cooldowns.clear();
    }

    private boolean canSend(CommandSender sender, String channel, String key) {
        if (!plugin.getConfig().getBoolean("feedback-cooldowns.enabled", true)) {
            return true;
        }

        if (!(sender instanceof Player)) {
            return !plugin.getConfig().getBoolean("feedback-cooldowns.apply-to-console", false);
        }

        Player player = (Player) sender;
        long duration = duration(channel, key);
        if (duration <= 0) {
            return true;
        }

        String cooldownKey = cooldownKey(player, channel, key);
        long now = System.currentTimeMillis();
        Long expires = cooldowns.get(cooldownKey);
        if (expires != null && expires > now) {
            if ("reset".equalsIgnoreCase(plugin.getConfig().getString("feedback-cooldowns.suppressed-behavior", "ignore"))) {
                cooldowns.put(cooldownKey, now + duration);
            }
            return false;
        }

        cooldowns.put(cooldownKey, now + duration);
        return true;
    }

    private long duration(String channel, String key) {
        String safeKey = key == null ? "default" : key.toLowerCase(Locale.ROOT);
        long specific = plugin.getConfig().getLong("feedback-cooldowns." + channel + ".per-key." + safeKey, -1);
        if (specific >= 0) {
            return specific;
        }
        return Math.max(0, plugin.getConfig().getLong("feedback-cooldowns." + channel + ".default-ms", 1000));
    }

    private String cooldownKey(Player player, String channel, String key) {
        String mode = plugin.getConfig().getString("feedback-cooldowns." + channel + ".mode", "per-key");
        String part = "global".equalsIgnoreCase(mode) ? "global" : (key == null ? "default" : key.toLowerCase(Locale.ROOT));
        return player.getUniqueId() + ":" + channel + ":" + part;
    }
}
