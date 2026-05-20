package org.flennn.lightkilleffects.permission;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.flennn.lightkilleffects.LightKillEffects;
import org.flennn.lightkilleffects.util.Console;

public class PermissionManager {
    private final LightKillEffects plugin;

    public PermissionManager(LightKillEffects plugin) {
        this.plugin = plugin;
    }

    public boolean hasBasicAccess(CommandSender sender) {
        if (hasPermission(sender, "killeffects.use")) {
            return true;
        }
        denyMessage(sender, "killeffects.use");
        return false;
    }

    public boolean canOpenGUI(CommandSender sender) {
        if (hasPermission(sender, "killeffects.gui") || 
            hasPermission(sender, "killeffects.gui.open")) {
            return true;
        }
        denyMessage(sender, "killeffects.gui");
        return false;
    }

    public boolean canBrowseCategories(CommandSender sender) {
        if (hasPermission(sender, "killeffects.gui.categories")) {
            return true;
        }
        denyMessage(sender, "killeffects.gui.categories");
        return false;
    }

    public boolean canManageFavorites(CommandSender sender) {
        if (hasPermission(sender, "killeffects.gui.favorites")) {
            return true;
        }
        denyMessage(sender, "killeffects.gui.favorites");
        return false;
    }

    public boolean canViewInfo(CommandSender sender) {
        if (hasPermission(sender, "killeffects.gui.info")) {
            return true;
        }
        denyMessage(sender, "killeffects.gui.info");
        return false;
    }

    public boolean canSetEffect(CommandSender sender) {
        if (hasPermission(sender, "killeffects.effect.set") ||
            hasPermission(sender, "killeffects.effect.use")) {
            return true;
        }
        denyMessage(sender, "killeffects.effect.set");
        return false;
    }

    public boolean canSetEffectForOthers(CommandSender sender) {
        if (hasPermission(sender, "killeffects.effect.set.others")) {
            return true;
        }
        denyMessage(sender, "killeffects.effect.set.others");
        return false;
    }

    public boolean canAccessEffect(CommandSender sender, String effectName) {
        if (hasPermission(sender, "killeffects.effect." + effectName.toLowerCase()) ||
            hasPermission(sender, "killeffects.effect.*")) {
            return true;
        }
        denyMessage(sender, "killeffects.effect." + effectName.toLowerCase());
        return false;
    }

    public boolean canPreview(CommandSender sender) {
        if (hasPermission(sender, "killeffects.preview.use") ||
            hasPermission(sender, "killeffects.preview.all")) {
            return true;
        }
        denyMessage(sender, "killeffects.preview.use");
        return false;
    }

    public boolean canPreviewAll(CommandSender sender) {
        if (hasPermission(sender, "killeffects.preview.all")) {
            return true;
        }
        return false;
    }

    public boolean canReload(CommandSender sender) {
        if (hasPermission(sender, "killeffects.reload")) {
            return true;
        }
        denyMessage(sender, "killeffects.reload");
        return false;
    }

    public boolean isAdmin(CommandSender sender) {
        if (hasPermission(sender, "killeffects.admin")) {
            return true;
        }
        return false;
    }

    public boolean canSeePreviewPlayers(CommandSender sender) {
        if (hasPermission(sender, "killeffects.admin.see-previews")) {
            return true;
        }
        return false;
    }

    public boolean hasPermission(CommandSender sender, String permission) {
        if (sender.isOp()) {
            return true;
        }

        if (sender.hasPermission(permission)) {
            return true;
        }

        if (permission.contains(".")) {
            String[] parts = permission.split("\\.");
            String wildcard = String.join(".", parts, 0, parts.length - 1) + ".*";
            if (sender.hasPermission(wildcard)) {
                return true;
            }
        }

        if (sender.hasPermission("killeffects.*")) {
            return true;
        }

        return false;
    }

    private void denyMessage(CommandSender sender, String permission) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            String message = plugin.getConfig().getString("messages.no-permission", 
                "&cYou do not have permission to do this.");
            
            if (plugin.isDebugMode()) {
                message += " &7(Permission: " + permission + ")";
            }
            
            player.sendMessage(Console.color(message));
        }
    }

    public void validatePermissions() {
        plugin.logInfo("Permission system loaded:");
        plugin.logInfo("  - Basic: killeffects.use");
        plugin.logInfo("  - GUI: killeffects.gui, killeffects.gui.*");
        plugin.logInfo("  - Effects: killeffects.effect.*, killeffects.effect.<name>");
        plugin.logInfo("  - Preview: killeffects.preview.*, killeffects.preview.all");
        plugin.logInfo("  - Admin: killeffects.reload, killeffects.admin");
    }
}
