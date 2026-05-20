package org.flennn.lightkilleffects.listener;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.flennn.lightkilleffects.LightKillEffects;
import org.flennn.lightkilleffects.effect.EffectType;
import org.flennn.lightkilleffects.storage.PlayerData;
public class KillListener implements Listener {
    
    private final LightKillEffects plugin;
    
    public KillListener(LightKillEffects plugin) {
        this.plugin = plugin;
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity victim = event.getEntity();
        Player killer = null;
        if (victim instanceof org.bukkit.entity.LivingEntity) {
            killer = ((org.bukkit.entity.LivingEntity) victim).getKiller();
        }
        if (killer == null) return;
        if ((!plugin.getSettings().allowMobKillEffects() || plugin.getConfig().getBoolean("general.players-only", false))
                && !(victim instanceof org.bukkit.entity.Player)) {
            return;
        }
        if (!plugin.isReady()) return;
        if (!killer.hasPermission("killeffects.use")) return;
        PlayerData.PlayerEffectData killerData = plugin.getPlayerData().getPlayerData(killer);
        EffectType selectedEffect = killerData.getSelectedEffect();
        if (selectedEffect == null) return;
        if (!plugin.getPlayerData().hasEffectPermission(killer, selectedEffect)) {
            plugin.logDebug("Player " + killer.getName() + " lacks permission for effect: " + selectedEffect.getConfigKey());
            return;
        }
        if (!killerData.hasUnlockedEffect(selectedEffect)) {
            plugin.logDebug("Player " + killer.getName() + " hasn't unlocked effect: " + selectedEffect.getConfigKey());
            return;
        }
        org.bukkit.Location deathLocation = victim.getLocation();
        
        plugin.logDebug("Triggering " + selectedEffect.getDisplayName() + " for " + killer.getName() + 
                       " killing " + victim.getType().name() + " at " + formatLocation(deathLocation));
        plugin.getEffectManager().executeEffect(killer, deathLocation, selectedEffect);
        plugin.getPlayerData().recordKill(killer.getUniqueId(), selectedEffect);
        if (plugin.getConfig().getBoolean("general.show-effect-notifications", true)) {
            String effectName = plugin.getEffectsConfig().getString("effects." + selectedEffect.getConfigKey() + ".name", 
                                                            selectedEffect.getDisplayName());
            killer.sendActionBar(net.kyori.adventure.text.Component.text(
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                            plugin.getMessage("effect-triggered", "effect", effectName))));
        }
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isReady()) return;
        
        plugin.logDebug("Loading data for player: " + player.getName());
        PlayerData.PlayerEffectData playerData = plugin.getPlayerData().getPlayerData(player);
        playerData.setName(player.getName());
        updatePlayerEffectPermissions(player, playerData);
        

        
        plugin.logDebug("Player data loaded for " + player.getName() + " with " + 
                       playerData.getUnlockedEffects().size() + " unlocked effects");
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isReady()) return;
        
        plugin.logDebug("Saving data for player: " + player.getName());
        plugin.getPlayerData().savePlayerData(player.getUniqueId());
        if (plugin.getConfig().getBoolean("performance.clear-cache-on-quit", false)) {
            plugin.getPlayerData().clearPlayerCache(player.getUniqueId());
        }
    }
    private void updatePlayerEffectPermissions(Player player, PlayerData.PlayerEffectData playerData) {
        boolean dataChanged = false;
        
        for (EffectType effect : EffectType.values()) {
            boolean hasPermission = plugin.getPlayerData().hasEffectPermission(player, effect);
            boolean hasUnlocked = playerData.hasUnlockedEffect(effect);
            
            if (hasPermission && !hasUnlocked) {
                playerData.addUnlockedEffect(effect);
                dataChanged = true;
                plugin.logDebug("Unlocked " + effect.getConfigKey() + " for " + player.getName());
                if (plugin.getConfig().getBoolean("general.notify-on-unlock", true)) {
                    String effectName = plugin.getEffectsConfig().getString("effects." + effect.getConfigKey() + ".name", 
                                                                    effect.getDisplayName());
                    player.sendMessage(plugin.getMessage("effect-unlocked", "effect", effectName));
                }
            } else if (!hasPermission && hasUnlocked && effect.requiresPermission()) {
                plugin.getPlayerData().lockEffect(player.getUniqueId(), effect);
                dataChanged = true;
                plugin.logDebug("Locked " + effect.getConfigKey() + " for " + player.getName());
                if (plugin.getConfig().getBoolean("general.notify-on-lock", true)) {
                    String effectName = plugin.getEffectsConfig().getString("effects." + effect.getConfigKey() + ".name", 
                                                                    effect.getDisplayName());
                    player.sendMessage(plugin.getMessage("effect-locked-notification", "effect", effectName));
                }
            }
        }
        if (dataChanged) {
            plugin.getPlayerData().savePlayerData(player.getUniqueId());
        }
    }
    private String formatLocation(org.bukkit.Location location) {
        return String.format("%.1f, %.1f, %.1f in %s", 
                location.getX(), location.getY(), location.getZ(), location.getWorld().getName());
    }
}
