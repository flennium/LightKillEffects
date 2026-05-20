package org.flennn.lightkilleffects.permission;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.flennn.lightkilleffects.LightKillEffects;

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
        String key = effectName == null ? "" : effectName.toLowerCase(java.util.Locale.ROOT);
        if (hasPermission(sender, "killeffects.use." + key) ||
            hasPermission(sender, "killeffects.use.*") ||
            hasPermission(sender, "killeffects.effect." + key) ||
            hasPermission(sender, "killeffects.effect.*")) {
            return true;
        }
        denyMessage(sender, "killeffects.use." + key);
        return false;
    }

    public boolean canPreview(CommandSender sender) {
        if (hasPermission(sender, "killeffects.preview") ||
            hasPermission(sender, "killeffects.preview.use") ||
            hasPermission(sender, "killeffects.preview.all")) {
            return true;
        }
        denyMessage(sender, "killeffects.preview");
        return false;
    }

    public boolean canPreviewAll(CommandSender sender) {
        return hasPermission(sender, "killeffects.preview.all");
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
        return hasPermission(sender, "killeffects.admin.see-previews");
    }

    public boolean hasPermission(CommandSender sender, String permission) {
        if (sender == null || permission == null || permission.isEmpty()) {
            return false;
        }

        if (sender.isOp()) {
            return true;
        }

        if (sender.hasPermission(permission)) {
            return true;
        }

        if (permission.contains(".")) {
            String[] parts = permission.split("\\.");
            String wildcard = String.join(".", java.util.Arrays.copyOf(parts, parts.length - 1)) + ".*";
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
            if (plugin.isDebugMode()) {
                plugin.sendRawMessage(player, "no-permission-" + permission, plugin.getMessage("no-permission") + " &7(Permission: " + permission + ")");
            } else {
                plugin.sendMessage(player, "no-permission");
            }
        }
    }

    public void validatePermissions() {
        plugin.logInfo("Permission system loaded:");
        plugin.logInfo("  - Basic: killeffects.use");
        plugin.logInfo("  - GUI: killeffects.gui, killeffects.gui.*");
        plugin.logInfo("  - Effects: killeffects.use.*, killeffects.use.<name>");
        plugin.logInfo("  - Preview: killeffects.preview.*, killeffects.preview.all");
        plugin.logInfo("  - Admin: killeffects.reload, killeffects.admin");
    }
}
