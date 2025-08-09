package org.flennn;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for entity death events and triggers kill effects
 */
public class KillListener implements Listener {
    
    private final LightKillEffects plugin;
    
    public KillListener(LightKillEffects plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle entity deaths and trigger kill effects for player kills
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity victim = event.getEntity();
        Player killer = null;
        
        // Check if victim is a LivingEntity to get killer
        if (victim instanceof org.bukkit.entity.LivingEntity) {
            killer = ((org.bukkit.entity.LivingEntity) victim).getKiller();
        }
        
        // Check if killer is a player
        if (killer == null) return;
        
        // Check if players-only mode is enabled and victim is not a player
        if (plugin.getConfig().getBoolean("general.players-only", false) && !(victim instanceof org.bukkit.entity.Player)) {
            return;
        }
        
        // Check if plugin is ready
        if (!plugin.isReady()) return;
        
        // Check if killer has permission to use kill effects
        if (!killer.hasPermission("killeffects.use")) return;
        
        // Get killer's data
        PlayerData.PlayerEffectData killerData = plugin.getPlayerData().getPlayerData(killer);
        
        // Check if killer has a selected effect
        EffectType selectedEffect = killerData.getSelectedEffect();
        if (selectedEffect == null) return;
        
        // Check if killer has permission for the specific effect
        if (!plugin.getPlayerData().hasEffectPermission(killer, selectedEffect)) {
            plugin.logDebug("Player " + killer.getName() + " lacks permission for effect: " + selectedEffect.getConfigKey());
            return;
        }
        
        // Check if killer has unlocked the effect
        if (!killerData.hasUnlockedEffect(selectedEffect)) {
            plugin.logDebug("Player " + killer.getName() + " hasn't unlocked effect: " + selectedEffect.getConfigKey());
            return;
        }
        
        // Get death location
        org.bukkit.Location deathLocation = victim.getLocation();
        
        plugin.logDebug("Triggering " + selectedEffect.getDisplayName() + " for " + killer.getName() + 
                       " killing " + victim.getType().name() + " at " + formatLocation(deathLocation));
        
        // Execute the kill effect
        plugin.getEffectManager().executeEffect(killer, deathLocation, selectedEffect);
        
        // Record kill statistics
        plugin.getPlayerData().recordKill(killer.getUniqueId(), selectedEffect);
        
        // Send effect notification (if enabled in config)
        if (plugin.getConfig().getBoolean("general.show-effect-notifications", true)) {
            String effectName = plugin.getConfig().getString("effects." + selectedEffect.getConfigKey() + ".name", 
                                                            selectedEffect.getDisplayName());
            killer.sendActionBar(net.kyori.adventure.text.Component.text(
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                            plugin.getMessage("effect-triggered", "effect", effectName))));
        }
    }
    
    /**
     * Handle player join - load their data and set up defaults if needed
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check if plugin is ready
        if (!plugin.isReady()) return;
        
        plugin.logDebug("Loading data for player: " + player.getName());
        
        // Load player data (this will create new data if player is new)
        PlayerData.PlayerEffectData playerData = plugin.getPlayerData().getPlayerData(player);
        
        // Update player name in case it changed
        playerData.setName(player.getName());
        
        // Check and unlock effects based on permissions
        updatePlayerEffectPermissions(player, playerData);
        

        
        plugin.logDebug("Player data loaded for " + player.getName() + " with " + 
                       playerData.getUnlockedEffects().size() + " unlocked effects");
    }
    
    /**
     * Handle player quit - save their data and clear cache if needed
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Check if plugin is ready
        if (!plugin.isReady()) return;
        
        plugin.logDebug("Saving data for player: " + player.getName());
        
        // Save player data
        plugin.getPlayerData().savePlayerData(player.getUniqueId());
        
        // Optionally clear cache to save memory (can be configured)
        if (plugin.getConfig().getBoolean("performance.clear-cache-on-quit", false)) {
            plugin.getPlayerData().clearPlayerCache(player.getUniqueId());
        }
    }
    
    /**
     * Update player's unlocked effects based on their current permissions
     */
    private void updatePlayerEffectPermissions(Player player, PlayerData.PlayerEffectData playerData) {
        boolean dataChanged = false;
        
        for (EffectType effect : EffectType.values()) {
            boolean hasPermission = plugin.getPlayerData().hasEffectPermission(player, effect);
            boolean hasUnlocked = playerData.hasUnlockedEffect(effect);
            
            if (hasPermission && !hasUnlocked) {
                // Player gained permission - unlock effect
                playerData.addUnlockedEffect(effect);
                dataChanged = true;
                plugin.logDebug("Unlocked " + effect.getConfigKey() + " for " + player.getName());
                
                // Notify player of new effect
                if (plugin.getConfig().getBoolean("general.notify-on-unlock", true)) {
                    String effectName = plugin.getConfig().getString("effects." + effect.getConfigKey() + ".name", 
                                                                    effect.getDisplayName());
                    player.sendMessage(plugin.getMessage("effect-unlocked", "effect", effectName));
                }
            } else if (!hasPermission && hasUnlocked && effect.requiresPermission()) {
                // Player lost permission - lock effect
                plugin.getPlayerData().lockEffect(player.getUniqueId(), effect);
                dataChanged = true;
                plugin.logDebug("Locked " + effect.getConfigKey() + " for " + player.getName());
                
                // Notify player of lost effect
                if (plugin.getConfig().getBoolean("general.notify-on-lock", true)) {
                    String effectName = plugin.getConfig().getString("effects." + effect.getConfigKey() + ".name", 
                                                                    effect.getDisplayName());
                    player.sendMessage(plugin.getMessage("effect-locked-notification", "effect", effectName));
                }
            }
        }
        
        // Save data if changes were made
        if (dataChanged) {
            plugin.getPlayerData().savePlayerData(player.getUniqueId());
        }
    }
    

    
    /**
     * Format location for debug messages
     */
    private String formatLocation(org.bukkit.Location location) {
        return String.format("%.1f, %.1f, %.1f in %s", 
                location.getX(), location.getY(), location.getZ(), location.getWorld().getName());
    }
}
