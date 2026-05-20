package org.flennn.lightkilleffects.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.flennn.lightkilleffects.LightKillEffects;
import org.flennn.lightkilleffects.effect.EffectType;
import org.flennn.lightkilleffects.storage.PlayerData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles all commands for the KillEffects plugin
 */
public class KillEffectCommand implements CommandExecutor, TabCompleter {
    
    private final LightKillEffects plugin;
    
    public KillEffectCommand(LightKillEffects plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if plugin is ready
        if (!plugin.isReady()) {
            sender.sendMessage(plugin.getMessage("plugin-not-ready"));
            return true;
        }
        
        // No arguments - show help or open GUI for players
        if (args.length == 0) {
            if (sender instanceof Player) {
                return handleGuiCommand((Player) sender);
            } else {
                sendHelpMessage(sender);
                return true;
            }
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "gui":
            case "menu":
                return handleGuiCommand(sender);
                
            case "set":
                return handleSetCommand(sender, args);
                
            case "preview":
                return handlePreviewCommand(sender, args);
                
            case "reload":
                return handleReloadCommand(sender);
                
            case "info":
                return handleInfoCommand(sender, args);
                
            case "stats":
                return handleStatsCommand(sender, args);
                
            case "favorite":
            case "fav":
                return handleFavoriteCommand(sender, args);
                
            case "help":
            default:
                sendHelpMessage(sender);
                return true;
        }
    }
    
    /**
     * Handle GUI command
     */
    private boolean handleGuiCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("players-only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("killeffects.gui")) {
            player.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }
        
        // Open GUI
        plugin.getEffectMenu().openMainMenu(player);
        player.sendMessage(plugin.getMessage("gui-opened"));
        
        return true;
    }
    
    /**
     * Handle set command
     */
    private boolean handleSetCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessage("usage-set"));
            return true;
        }
        
        Player target;
        String effectName;
        
        // Check if setting for another player
        if (args.length >= 3 && sender.hasPermission("killeffects.set.others")) {
            if (!(sender instanceof Player) && !plugin.getSettings().allowConsoleSetOthers()) {
                sender.sendMessage(plugin.getMessage("players-only"));
                return true;
            }
            target = plugin.getServer().getPlayer(args[1]);
            effectName = args[2];
            
            if (target == null) {
                sender.sendMessage(plugin.getMessage("player-not-found", "player", args[1]));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getMessage("players-only"));
                return true;
            }
            target = (Player) sender;
            effectName = args[1];
        }
        
        // Check permission
        if (!target.hasPermission("killeffects.use")) {
            sender.sendMessage(plugin.getMessage("target-no-permission", "player", target.getName()));
            return true;
        }
        
        // Handle "none" to disable effect
        if (effectName.equalsIgnoreCase("none") || effectName.equalsIgnoreCase("off")) {
            PlayerData.PlayerEffectData playerData = plugin.getPlayerData().getPlayerData(target);
            playerData.setSelectedEffect(null);
            plugin.getPlayerData().savePlayerData(target.getUniqueId());
            
            target.sendMessage(plugin.getMessage("effect-disabled"));
            if (target != sender) {
                sender.sendMessage(plugin.getMessage("effect-disabled-other", "player", target.getName()));
            }
            return true;
        }
        
        // Find effect
        EffectType effect = EffectType.fromConfigKey(effectName);
        if (effect == null) {
            effect = EffectType.fromDisplayName(effectName);
        }
        
        if (effect == null) {
            sender.sendMessage(plugin.getMessage("effect-not-found", "effect", effectName));
            return true;
        }
        
        // Check permissions
        if (!plugin.getPlayerData().hasEffectPermission(target, effect)) {
            sender.sendMessage(plugin.getMessage("effect-no-permission", "effect", effect.getDisplayName()));
            return true;
        }
        
        // Check if unlocked
        PlayerData.PlayerEffectData playerData = plugin.getPlayerData().getPlayerData(target);
        if (!playerData.hasUnlockedEffect(effect)) {
            sender.sendMessage(plugin.getMessage("effect-locked", "effect", effect.getDisplayName()));
            return true;
        }
        
        // Set effect
        playerData.setSelectedEffect(effect);
        plugin.getPlayerData().savePlayerData(target.getUniqueId());
        
        String effectDisplayName = plugin.getConfig().getString("effects." + effect.getConfigKey() + ".name", 
                                                                effect.getDisplayName());
        
        target.sendMessage(plugin.getMessage("effect-set", "effect", effectDisplayName));
        if (target != sender) {
            sender.sendMessage(plugin.getMessage("effect-set-other", "player", target.getName())
                    .replace("{effect}", effectDisplayName));
        }
        
        return true;
    }
    
    /**
     * Handle preview command
     */
    private boolean handlePreviewCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("players-only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length < 2) {
            player.sendMessage(plugin.getMessage("usage-preview"));
            return true;
        }
        
        // Find effect
        EffectType effect = EffectType.fromConfigKey(args[1]);
        if (effect == null) {
            effect = EffectType.fromDisplayName(args[1]);
        }
        
        if (effect == null) {
            player.sendMessage(plugin.getMessage("effect-not-found", "effect", args[1]));
            return true;
        }
        
        // Check if previews are enabled
        if (!plugin.getConfig().getBoolean("gui.preview.enabled", true)) {
            player.sendMessage(plugin.getMessage("preview-disabled"));
            return true;
        }
        
        // Check preview cooldown
        if (plugin.getEffectMenu().isPlayerOnPreviewCooldown(player)) {
            long remaining = plugin.getEffectMenu().getPlayerRemainingPreviewCooldown(player);
            player.sendMessage(plugin.getMessage("preview-cooldown", "seconds", String.valueOf(remaining)));
            return true;
        }
        
        // Check preview permission
        if (!player.hasPermission("killeffects.preview.all") && 
            !plugin.getPlayerData().hasEffectPermission(player, effect)) {
            player.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        if (plugin.getSettings().requirePreviewUnlock()
                && !player.hasPermission("killeffects.preview.all")
                && !plugin.getPlayerData().getPlayerData(player).hasUnlockedEffect(effect)) {
            player.sendMessage(plugin.getMessage("effect-locked", "effect", effect.getDisplayName()));
            return true;
        }
        
        // Execute preview
        org.bukkit.Location previewLocation = player.getLocation().add(
                player.getLocation().getDirection().multiply(
                        plugin.getConfig().getInt("gui.preview.location-offset", 3)
                )
        );
        
        // Set cooldown before executing
        plugin.getEffectMenu().setPlayerPreviewCooldown(player);
        plugin.getEffectManager().executeEffect(player, previewLocation, effect);
        
        String effectDisplayName = plugin.getConfig().getString("effects." + effect.getConfigKey() + ".name", 
                                                                effect.getDisplayName());
        player.sendMessage(plugin.getMessage("effect-previewed", "effect", effectDisplayName));
        
        return true;
    }
    
    /**
     * Handle reload command
     */
    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("killeffects.reload")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }
        
        try {
            plugin.reloadPlugin();
            sender.sendMessage(plugin.getMessage("config-reloaded"));
        } catch (Exception e) {
            sender.sendMessage(plugin.getMessage("reload-error"));
            plugin.getLogger().severe("Error during reload: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
    
    /**
     * Handle info command
     */
    private boolean handleInfoCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("players-only"));
            return true;
        }
        
        Player player = (Player) sender;
        PlayerData.PlayerEffectData playerData = plugin.getPlayerData().getPlayerData(player);
        
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&6&lKill Effects Info:"));
        
        String currentEffect = playerData.getSelectedEffect() != null ? 
                plugin.getConfig().getString("effects." + playerData.getSelectedEffect().getConfigKey() + ".name", 
                                            playerData.getSelectedEffect().getDisplayName()) : "None";
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Current Effect: &e" + currentEffect));
        
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&7Unlocked Effects: &e" + playerData.getUnlockedEffects().size() + "&7/&e" + EffectType.values().length));
        
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&7Total Kills: &e" + playerData.getTotalKills()));
        
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&7Favorites: &e" + playerData.getFavorites().size() + "&7/&e" + 
                plugin.getConfig().getInt("general.max-favorites", 5)));
        
        return true;
    }
    
    /**
     * Handle stats command
     */
    private boolean handleStatsCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("players-only"));
            return true;
        }
        
        Player player = (Player) sender;
        PlayerData.PlayerEffectData playerData = plugin.getPlayerData().getPlayerData(player);
        
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&6&lYour Effect Statistics:"));
        
        if (playerData.getEffectStats().isEmpty()) {
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7No effects used yet!"));
            return true;
        }
        
        // Show top 5 most used effects
        playerData.getEffectStats().entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(5)
                .forEach(entry -> {
                    String effectName = plugin.getConfig().getString("effects." + entry.getKey().getConfigKey() + ".name", 
                                                                    entry.getKey().getDisplayName());
                    String message = "&7" + effectName + ": &e" + entry.getValue() + " kills";
                    player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
                });
        
        return true;
    }
    
    /**
     * Handle favorite command
     */
    private boolean handleFavoriteCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("players-only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length < 3) {
            player.sendMessage(plugin.getMessage("usage-favorite"));
            return true;
        }
        
        String action = args[1].toLowerCase();
        String effectName = args[2];
        
        // Find effect
        EffectType effect = EffectType.fromConfigKey(effectName);
        if (effect == null) {
            effect = EffectType.fromDisplayName(effectName);
        }
        
        if (effect == null) {
            player.sendMessage(plugin.getMessage("effect-not-found", "effect", effectName));
            return true;
        }
        
        PlayerData.PlayerEffectData playerData = plugin.getPlayerData().getPlayerData(player);
        
        // Check if player has the effect unlocked
        if (!playerData.hasUnlockedEffect(effect)) {
            player.sendMessage(plugin.getMessage("effect-locked", "effect", effect.getDisplayName()));
            return true;
        }
        
        String effectDisplayName = plugin.getConfig().getString("effects." + effect.getConfigKey() + ".name", 
                                                                effect.getDisplayName());
        
        switch (action) {
            case "add":
                if (playerData.isFavorite(effect)) {
                    player.sendMessage(plugin.getMessage("already-favorite", "effect", effectDisplayName));
                } else if (!playerData.canAddFavorite()) {
                    player.sendMessage(plugin.getMessage("favorites-full"));
                } else {
                    playerData.addFavorite(effect);
                    plugin.getPlayerData().savePlayerData(player.getUniqueId());
                    player.sendMessage(plugin.getMessage("favorite-added", "effect", effectDisplayName));
                }
                break;
                
            case "remove":
                if (!playerData.isFavorite(effect)) {
                    player.sendMessage(plugin.getMessage("not-favorite", "effect", effectDisplayName));
                } else {
                    playerData.removeFavorite(effect);
                    plugin.getPlayerData().savePlayerData(player.getUniqueId());
                    player.sendMessage(plugin.getMessage("favorite-removed", "effect", effectDisplayName));
                }
                break;
                
            default:
                player.sendMessage(plugin.getMessage("usage-favorite"));
                break;
        }
        
        return true;
    }
    
    /**
     * Send help message
     */
    private void sendHelpMessage(CommandSender sender) {
        List<String> helpLines = plugin.getConfig().getStringList("messages.help");
        for (String line : helpLines) {
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Main subcommands
            completions.addAll(Arrays.asList("gui", "set", "preview", "reload", "info", "stats", "favorite", "help"));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "set":
                case "preview":
                    // Effect names
                    completions.add("none");
                    for (EffectType effect : EffectType.values()) {
                        completions.add(effect.getConfigKey());
                    }
                    break;
                    
                case "favorite":
                    completions.addAll(Arrays.asList("add", "remove"));
                    break;
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            if ("set".equals(subCommand) && sender.hasPermission("killeffects.set.others")) {
                // Player names for setting others' effects
                return plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            } else if ("favorite".equals(subCommand)) {
                // Effect names for favorites
                for (EffectType effect : EffectType.values()) {
                    completions.add(effect.getConfigKey());
                }
            }
        } else if (args.length == 4 && "set".equals(args[0].toLowerCase()) && sender.hasPermission("killeffects.set.others")) {
            // Effect names when setting for other players
            completions.add("none");
            for (EffectType effect : EffectType.values()) {
                completions.add(effect.getConfigKey());
            }
        }
        
        // Filter completions based on what the player has typed
        String partial = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
    }
}
