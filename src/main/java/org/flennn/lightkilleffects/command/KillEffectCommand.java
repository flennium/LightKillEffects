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
public class KillEffectCommand implements CommandExecutor, TabCompleter {
    
    private final LightKillEffects plugin;
    
    public KillEffectCommand(LightKillEffects plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.isReady()) {
            plugin.sendMessage(sender, "plugin-not-ready");
            return true;
        }
        if (sender instanceof Player && plugin.getPreviewManager().isInPreview((Player) sender)
                && (args.length == 0 || "exit".equalsIgnoreCase(args[0]) || "stop".equalsIgnoreCase(args[0]))) {
            plugin.getPreviewManager().endPreview((Player) sender);
            plugin.sendMessage(sender, "preview-ended");
            return true;
        }
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
    private boolean handleGuiCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            plugin.sendMessage(sender, "players-only");
            return true;
        }
        
        Player player = (Player) sender;
        if (!player.hasPermission("killeffects.gui")) {
            plugin.sendMessage(player, "no-permission");
            return true;
        }
        plugin.getEffectMenu().openMainMenu(player);
        plugin.sendMessage(player, "gui-opened");
        
        return true;
    }
    private boolean handleSetCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.sendMessage(sender, "usage-set");
            return true;
        }
        
        Player target;
        String effectName;
        if (args.length >= 3 && sender.hasPermission("killeffects.set.others")) {
            if (!(sender instanceof Player) && !plugin.getSettings().allowConsoleSetOthers()) {
                plugin.sendMessage(sender, "players-only");
                return true;
            }
            target = plugin.getServer().getPlayer(args[1]);
            effectName = args[2];
            
            if (target == null) {
                plugin.sendMessage(sender, "player-not-found", "player", args[1]);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                plugin.sendMessage(sender, "players-only");
                return true;
            }
            target = (Player) sender;
            effectName = args[1];
        }
        if (!target.hasPermission("killeffects.use")) {
            plugin.sendMessage(sender, "target-no-permission", "player", target.getName());
            return true;
        }
        if (effectName.equalsIgnoreCase("none") || effectName.equalsIgnoreCase("off")) {
            PlayerData.PlayerEffectData playerData = plugin.getPlayerData().getPlayerData(target);
            playerData.setSelectedEffect(null);
            plugin.getPlayerData().savePlayerData(target.getUniqueId());
            
            plugin.sendMessage(target, "effect-disabled");
            if (target != sender) {
                plugin.sendMessage(sender, "effect-disabled-other", "player", target.getName());
            }
            return true;
        }
        EffectType effect = EffectType.fromConfigKey(effectName);
        if (effect == null) {
            effect = EffectType.fromDisplayName(effectName);
        }
        
        if (effect == null) {
            plugin.sendMessage(sender, "effect-not-found", "effect", effectName);
            return true;
        }
        if (!plugin.getPlayerData().hasEffectPermission(target, effect)) {
            plugin.sendMessage(sender, "effect-no-permission", "effect", effect.getDisplayName());
            return true;
        }
        PlayerData.PlayerEffectData playerData = plugin.getPlayerData().getPlayerData(target);
        if (!playerData.hasUnlockedEffect(effect)) {
            plugin.sendMessage(sender, "effect-locked", "effect", effect.getDisplayName());
            return true;
        }
        playerData.setSelectedEffect(effect);
        plugin.getPlayerData().savePlayerData(target.getUniqueId());
        
        String effectDisplayName = plugin.getEffectsConfig().getString("effects." + effect.getConfigKey() + ".name", 
                                                                effect.getDisplayName());
        
        plugin.sendMessage(target, "effect-set", "effect", effectDisplayName);
        if (target != sender) {
            plugin.sendMessage(sender, "effect-set-other", "player", target.getName(), "effect", effectDisplayName);
        }
        
        return true;
    }
    private boolean handlePreviewCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.sendMessage(sender, "players-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length < 2) {
            plugin.sendMessage(player, "usage-preview");
            return true;
        }

        if (!plugin.getPermissionManager().canPreview(player)) {
            return true;
        }

        EffectType effect = EffectType.fromConfigKey(args[1]);
        if (effect == null) {
            effect = EffectType.fromDisplayName(args[1]);
        }
        
        if (effect == null) {
            plugin.sendMessage(player, "effect-not-found", "effect", args[1]);
            return true;
        }

        if (!plugin.getPermissionManager().canPreviewAll(player)
                && !plugin.getPermissionManager().canAccessEffect(player, effect.getConfigKey())) {
            return true;
        }

        if (plugin.getSettings().requirePreviewUnlock()
                && !plugin.getPermissionManager().canPreviewAll(player)
                && !plugin.getPlayerData().getPlayerData(player).hasUnlockedEffect(effect)) {
            plugin.sendMessage(player, "effect-locked", "effect", effect.getDisplayName());
            return true;
        }

        if (plugin.getPreviewManager().isOnCooldown(player)) {
            long remaining = plugin.getPreviewManager().getRemainingCooldown(player);
            plugin.sendMessage(player, "preview-cooldown", "seconds", String.valueOf(remaining));
            return true;
        }

        if (plugin.getPreviewManager().startPreview(player, effect.getDisplayName())) {
            org.bukkit.Location effectLoc = player.getLocation().add(
                    player.getLocation().getDirection().multiply(
                            plugin.getConfig().getInt("gui.preview.location-offset", 3)
                    )
            );
            plugin.getEffectManager().executeEffect(player, effectLoc, effect);
            
            String effectDisplayName = plugin.getEffectsConfig().getString("effects." + effect.getConfigKey() + ".name", 
                                                                    effect.getDisplayName());
            plugin.sendMessage(player, "effect-previewed", "effect", effectDisplayName);
        }
        
        return true;
    }
    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("killeffects.reload")) {
            plugin.sendMessage(sender, "no-permission");
            return true;
        }
        
        try {
            plugin.reloadPlugin();
            plugin.sendMessage(sender, "config-reloaded");
        } catch (Exception e) {
            plugin.sendMessage(sender, "reload-error");
            plugin.getLogger().severe("Error during reload: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
    private boolean handleInfoCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.sendMessage(sender, "players-only");
            return true;
        }
        
        Player player = (Player) sender;
        PlayerData.PlayerEffectData playerData = plugin.getPlayerData().getPlayerData(player);
        
        plugin.sendRawMessage(player, "info-header", "&6&lKill Effects Info:");
        
        String currentEffect = playerData.getSelectedEffect() != null ? 
                plugin.getEffectsConfig().getString("effects." + playerData.getSelectedEffect().getConfigKey() + ".name", 
                                            playerData.getSelectedEffect().getDisplayName()) : "None";
        plugin.sendRawMessage(player, "info-current-effect", "&7Current Effect: &e" + currentEffect);
        
        plugin.sendRawMessage(player, "info-unlocked-effects", 
                "&7Unlocked Effects: &e" + playerData.getUnlockedEffects().size() + "&7/&e" + EffectType.values().length);
        
        plugin.sendRawMessage(player, "info-total-kills", "&7Total Kills: &e" + playerData.getTotalKills());
        
        plugin.sendRawMessage(player, "info-favorites", 
                "&7Favorites: &e" + playerData.getFavorites().size() + "&7/&e" + 
                plugin.getConfig().getInt("general.max-favorites", 5));
        
        return true;
    }
    private boolean handleStatsCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.sendMessage(sender, "players-only");
            return true;
        }
        
        Player player = (Player) sender;
        PlayerData.PlayerEffectData playerData = plugin.getPlayerData().getPlayerData(player);
        
        plugin.sendRawMessage(player, "stats-header", "&6&lYour Effect Statistics:");
        
        if (playerData.getEffectStats().isEmpty()) {
            plugin.sendRawMessage(player, "stats-empty", "&7No effects used yet!");
            return true;
        }
        playerData.getEffectStats().entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(5)
                .forEach(entry -> {
                    String effectName = plugin.getEffectsConfig().getString("effects." + entry.getKey().getConfigKey() + ".name", 
                                                                    entry.getKey().getDisplayName());
                    String message = "&7" + effectName + ": &e" + entry.getValue() + " kills";
                    plugin.sendRawMessage(player, "stats-effect-" + entry.getKey().getConfigKey(), message);
                });
        
        return true;
    }
    private boolean handleFavoriteCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.sendMessage(sender, "players-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length < 3) {
            plugin.sendMessage(player, "usage-favorite");
            return true;
        }
        
        String action = args[1].toLowerCase();
        String effectName = args[2];
        EffectType effect = EffectType.fromConfigKey(effectName);
        if (effect == null) {
            effect = EffectType.fromDisplayName(effectName);
        }
        
        if (effect == null) {
            plugin.sendMessage(player, "effect-not-found", "effect", effectName);
            return true;
        }
        
        PlayerData.PlayerEffectData playerData = plugin.getPlayerData().getPlayerData(player);
        if (!playerData.hasUnlockedEffect(effect)) {
            plugin.sendMessage(player, "effect-locked", "effect", effect.getDisplayName());
            return true;
        }
        
        String effectDisplayName = plugin.getEffectsConfig().getString("effects." + effect.getConfigKey() + ".name", 
                                                                effect.getDisplayName());
        
        switch (action) {
            case "add":
                if (playerData.isFavorite(effect)) {
                    plugin.sendMessage(player, "already-favorite", "effect", effectDisplayName);
                } else if (!playerData.canAddFavorite()) {
                    plugin.sendMessage(player, "favorites-full");
                } else {
                    playerData.addFavorite(effect);
                    plugin.getPlayerData().savePlayerData(player.getUniqueId());
                    plugin.sendMessage(player, "favorite-added", "effect", effectDisplayName);
                }
                break;
                
            case "remove":
                if (!playerData.isFavorite(effect)) {
                    plugin.sendMessage(player, "not-favorite", "effect", effectDisplayName);
                } else {
                    playerData.removeFavorite(effect);
                    plugin.getPlayerData().savePlayerData(player.getUniqueId());
                    plugin.sendMessage(player, "favorite-removed", "effect", effectDisplayName);
                }
                break;
                
            default:
                plugin.sendMessage(player, "usage-favorite");
                break;
        }
        
        return true;
    }
    private void sendHelpMessage(CommandSender sender) {
        List<String> helpLines = plugin.getMessagesConfig().getStringList("messages.help");
        int index = 0;
        for (String line : helpLines) {
            plugin.sendRawMessage(sender, "help-" + index, line);
            index++;
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("gui", "set", "preview", "reload", "info", "stats", "favorite", "help"));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "set":
                case "preview":
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
                return plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            } else if ("favorite".equals(subCommand)) {
                for (EffectType effect : EffectType.values()) {
                    completions.add(effect.getConfigKey());
                }
            }
        } else if (args.length == 4 && "set".equals(args[0].toLowerCase()) && sender.hasPermission("killeffects.set.others")) {
            completions.add("none");
            for (EffectType effect : EffectType.values()) {
                completions.add(effect.getConfigKey());
            }
        }
        String partial = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
    }
}
