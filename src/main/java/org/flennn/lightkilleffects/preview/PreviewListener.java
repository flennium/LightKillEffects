package org.flennn.lightkilleffects.preview;

import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.flennn.lightkilleffects.LightKillEffects;
import org.flennn.lightkilleffects.util.Console;

public class PreviewListener implements Listener {
    private final LightKillEffects plugin;
    private final PreviewEnvironmentManager previewManager;

    public PreviewListener(LightKillEffects plugin, PreviewEnvironmentManager previewManager) {
        this.plugin = plugin;
        this.previewManager = previewManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getExited();
        if (previewManager.isInPreview(player)) {
            event.setCancelled(true);
            plugin.logDebug("Blocked dismount attempt for " + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!previewManager.isInPreview(player)) {
            return;
        }

        String command = event.getMessage().toLowerCase();

        if (command.startsWith("/tp ") || command.startsWith("/teleport ") ||
                command.startsWith("/warp ") || command.startsWith("/home ") ||
                command.startsWith("/spawn ") || command.startsWith("/back ")) {
            event.setCancelled(true);
            plugin.sendMessage(player, "preview-blocked-command");
            plugin.logDebug("Blocked command during preview: " + command);
            return;
        }

        if (command.startsWith("/quit") || command.startsWith("/logout")) {
            event.setCancelled(true);
            plugin.sendMessage(player, "preview-cannot-quit");
            plugin.logDebug("Blocked quit attempt during preview for " + player.getName());
            return;
        }

        if (command.startsWith("/") && !isSafeCommand(command)) {
            String[] parts = command.split(" ");
            String cmd = parts[0].substring(1).toLowerCase();

            if (!isWhitelistedCommand(cmd)) {
                event.setCancelled(true);
                plugin.sendMessage(player, "preview-commands-disabled");
                plugin.logDebug("Blocked command during preview: " + command);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        if (previewManager.isInPreview(player)) {
            event.setCancelled(true);
            plugin.logDebug("Blocked damage during preview for " + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (previewManager.isInPreview(player)) {
            event.setDeathMessage(null);
            previewManager.endPreview(player);
            plugin.logDebug("Ended preview due to player death: " + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PreviewSession session = previewManager.getSession(player);
        
        if (session != null) {
            event.setRespawnLocation(session.getPreviewLocation());
            plugin.logDebug("Respawned player in preview location: " + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (!previewManager.isInPreview(player)) {
            return;
        }

        PreviewSession session = previewManager.getSession(player);
        if (session != null) {
            if (!event.getTo().getWorld().equals(session.getPreviewLocation().getWorld())) {
                event.setCancelled(true);
                plugin.sendMessage(player, "preview-cannot-teleport");
                plugin.logDebug("Blocked teleport attempt during preview: " + player.getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (previewManager.isInPreview(player)) {
            event.setCancelled(true);
            plugin.logDebug("Blocked interaction during preview: " + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (previewManager.isInPreview(player)) {
            event.setCancelled(true);
            plugin.logDebug("Blocked entity interaction during preview: " + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (previewManager.isInPreview(player)) {
            event.setCancelled(true);
            plugin.logDebug("Blocked block break during preview: " + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (previewManager.isInPreview(player)) {
            event.setCancelled(true);
            plugin.logDebug("Blocked block place during preview: " + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            Player player = (Player) entity;
            if (previewManager.isInPreview(player)) {
                event.setCancelled(true);
                plugin.logDebug("Blocked portal creation during preview: " + player.getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        previewManager.removeStaleSession(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (previewManager.isInPreview(player)) {
            event.setCancelled(true);
            plugin.logDebug("Blocked item drop during preview: " + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(org.bukkit.event.inventory.InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            if (previewManager.isInPreview(player)) {
                event.setCancelled(true);
                plugin.logDebug("Blocked inventory open during preview: " + player.getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PreviewSession session = previewManager.getSession(player);
        if (session == null || !session.isActive()) {
            return;
        }

        previewManager.validateBoatIntegrity(player);
        if (event.getTo() == null || event.getTo().getWorld() == null || session.getPreviewLocation().getWorld() == null) {
            return;
        }

        if (!event.getTo().getWorld().equals(session.getPreviewLocation().getWorld())
                || hasPositionChanged(event)) {
            event.setTo(lockedPreviewLocation(session, event.getTo()));
            plugin.logDebug("Locked preview movement for " + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof Boat) || event.getTo() == null) {
            return;
        }

        for (Entity passenger : event.getVehicle().getPassengers()) {
            if (!(passenger instanceof Player)) {
                continue;
            }

            Player player = (Player) passenger;
            PreviewSession session = previewManager.getSession(player);
            if (session == null || !session.isActive()) {
                continue;
            }

            Location anchor = session.getPreviewLocation().clone().add(0, 0.5, 0);
            anchor.setYaw(event.getTo().getYaw());
            anchor.setPitch(event.getTo().getPitch());
            if (!event.getTo().getWorld().equals(anchor.getWorld())
                    || event.getTo().distanceSquared(anchor) > 0.0001D) {
                event.getVehicle().teleport(anchor);
                plugin.logDebug("Reset preview boat position for " + player.getName());
            }
        }
    }

    private boolean hasPositionChanged(PlayerMoveEvent event) {
        if (event.getFrom() == null || event.getTo() == null) {
            return false;
        }
        return event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ();
    }

    private org.bukkit.Location lockedPreviewLocation(PreviewSession session, org.bukkit.Location target) {
        org.bukkit.Location locked = session.getPreviewLocation().clone();
        locked.setYaw(target.getYaw());
        locked.setPitch(target.getPitch());
        return locked;
    }

    private boolean isSafeCommand(String command) {
        String lower = command.toLowerCase();
        return lower.startsWith("/me ") || lower.startsWith("/say ");
    }

    private boolean isWhitelistedCommand(String cmd) {
        String[] whitelist = {
            "me", "say", "killeffects", "ke", "effects"
        };
        for (String w : whitelist) {
            if (cmd.equals(w)) {
                return true;
            }
        }
        return false;
    }
}
