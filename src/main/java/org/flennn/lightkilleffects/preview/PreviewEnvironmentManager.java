package org.flennn.lightkilleffects.preview;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.flennn.lightkilleffects.LightKillEffects;
import org.flennn.lightkilleffects.util.Console;

import java.util.*;

public class PreviewEnvironmentManager {
    private final LightKillEffects plugin;
    private final Map<UUID, PreviewSession> activeSessions;
    private final Map<UUID, Long> previewCooldowns;

    public PreviewEnvironmentManager(LightKillEffects plugin) {
        this.plugin = plugin;
        this.activeSessions = new HashMap<>();
        this.previewCooldowns = new HashMap<>();
    }

    public boolean startPreview(Player player, String effectName) {
        if (!isPreviewEnabled()) {
            player.sendMessage(plugin.getMessage("preview-disabled"));
            return false;
        }

        if (isInPreview(player)) {
            player.sendMessage(plugin.getMessage("already-in-preview"));
            return false;
        }

        Location previewLoc = getPreviewLocation();
        if (previewLoc == null) {
            player.sendMessage(plugin.getMessage("preview-location-not-set"));
            plugin.logWarn("Preview location not configured in config.yml");
            return false;
        }

        try {
            PreviewSession session = new PreviewSession(player, previewLoc);
            
            savePlayerState(player);
            player.teleport(previewLoc);
            
            if (!mountInBoat(player)) {
                player.sendMessage(plugin.getMessage("preview-mount-failed"));
                restorePlayerState(player);
                return false;
            }
            
            activeSessions.put(player.getUniqueId(), session);
            setPreviewCooldown(player);
            hidePlayerFromOthers(player);
            
            plugin.logDebug("Started preview session for " + player.getName() + ": " + effectName);
            return true;
        } catch (Exception e) {
            Console.error("Failed to start preview for " + player.getName() + ": " + e.getMessage());
            plugin.getLogger().severe(e.toString());
            restorePlayerState(player);
            return false;
        }
    }

    private boolean mountInBoat(Player player) {
        try {
            Location boatLoc = player.getLocation().clone();
            boatLoc.setY(boatLoc.getY() + 0.5);

            Boat boat = player.getWorld().spawn(boatLoc, Boat.class);
            boat.setInvulnerable(true);
            boat.setCustomNameVisible(false);
            boat.setInvisible(true);
            boat.setNoGravity(true);
            boat.setPersistent(true);
            boat.addPassenger(player);

            PreviewSession session = activeSessions.get(player.getUniqueId());
            if (session != null) {
                session.setBoatEntity(boat);
                session.setBoatPlaced(true);
            }

            return true;
        } catch (Exception e) {
            Console.error("Failed to mount player in boat: " + e.getMessage());
            return false;
        }
    }

    public void endPreview(Player player) {
        PreviewSession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }

        try {
            if (session.isBoatValid()) {
                session.getBoatEntity().eject();
                session.getBoatEntity().remove();
            }

            Location originalLoc = session.getOriginalLocation();
            if (originalLoc != null && originalLoc.getWorld() != null) {
                player.teleport(originalLoc);
            }

            restorePlayerState(player);
            showPlayerToOthers(player);
            activeSessions.remove(player.getUniqueId());
            
            plugin.logDebug("Ended preview session for " + player.getName());
        } catch (Exception e) {
            Console.error("Error ending preview for " + player.getName() + ": " + e.getMessage());
        }
    }

    public boolean isInPreview(Player player) {
        PreviewSession session = activeSessions.get(player.getUniqueId());
        return session != null && session.isActive();
    }

    public boolean isPreviewEnabled() {
        return plugin.getConfig().getBoolean("preview.enabled", true);
    }

    public PreviewSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    public Collection<PreviewSession> getActiveSessions() {
        return new ArrayList<>(activeSessions.values());
    }

    private void savePlayerState(Player player) {
    }

    private void restorePlayerState(Player player) {
    }

    private void hidePlayerFromOthers(Player player) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.equals(player) && !other.hasPermission("killeffects.admin.see-previews")) {
                other.hidePlayer(plugin, player);
            }
        }
    }

    private void showPlayerToOthers(Player player) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.equals(player)) {
                other.showPlayer(plugin, player);
            }
        }
    }

    public boolean isOnCooldown(Player player) {
        UUID id = player.getUniqueId();
        if (!previewCooldowns.containsKey(id)) {
            return false;
        }

        long cooldownEnd = previewCooldowns.get(id);
        if (System.currentTimeMillis() >= cooldownEnd) {
            previewCooldowns.remove(id);
            return false;
        }
        return true;
    }

    public long getRemainingCooldown(Player player) {
        UUID id = player.getUniqueId();
        if (!previewCooldowns.containsKey(id)) {
            return 0;
        }

        long remaining = previewCooldowns.get(id) - System.currentTimeMillis();
        return Math.max(0, (remaining + 999) / 1000);
    }

    private void setPreviewCooldown(Player player) {
        long cooldownSeconds = plugin.getConfig().getLong("gui.preview.cooldown", 5);
        long cooldownMs = cooldownSeconds * 1000;
        previewCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + cooldownMs);
    }

    private Location getPreviewLocation() {
        String world = plugin.getConfig().getString("preview.world");
        if (world == null || world.isEmpty()) {
            return Bukkit.getWorld("world").getSpawnLocation();
        }

        org.bukkit.World w = Bukkit.getWorld(world);
        if (w == null) {
            Console.warn("Preview world '" + world + "' not found. Using default world.");
            return Bukkit.getWorld("world").getSpawnLocation();
        }

        double x = plugin.getConfig().getDouble("preview.x", 0);
        double y = plugin.getConfig().getDouble("preview.y", 100);
        double z = plugin.getConfig().getDouble("preview.z", 0);

        return new Location(w, x, y, z);
    }

    public void cleanup() {
        for (PreviewSession session : new ArrayList<>(activeSessions.values())) {
            Player player = Bukkit.getPlayer(session.getPlayerId());
            if (player != null) {
                endPreview(player);
            }
        }
        activeSessions.clear();
        previewCooldowns.clear();
    }

    public void removeStaleSession(UUID playerId) {
        PreviewSession session = activeSessions.get(playerId);
        if (session != null) {
            session.end();
            activeSessions.remove(playerId);
            plugin.logDebug("Cleaned up preview session for disconnected player: " + session.getPlayerName());
        }
    }

    public void validateBoatIntegrity(Player player) {
        PreviewSession session = activeSessions.get(player.getUniqueId());
        if (session != null && !session.isBoatValid()) {
            plugin.logDebug("Boat was removed; ending preview for " + player.getName());
            endPreview(player);
        }
    }

    public boolean isEnabled() {
        return isPreviewEnabled();
    }
}
