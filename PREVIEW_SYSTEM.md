# LightKillEffects - Enhanced Preview System Documentation

## Overview

The enhanced preview system creates isolated, fully controlled environments where players can safely preview kill effects without seeing or interacting with other players.

## Features Implemented

### 1. Isolated Preview Environment
- Players are teleported to a dedicated preview location
- Mounted inside an invisible, immovable boat
- Completely hidden from all other players
- Cannot see or interact with other players in any way

### 2. Complete Restriction System
The preview environment behaves like a private limbo instance with comprehensive restrictions:

#### Player Isolation
- Players are hidden from other players during previews
- Other players cannot see preview particles
- No player-to-player interaction possible
- Admins can optionally see previews with `killeffects.admin.see-previews` permission

#### Movement Prevention
- Cannot dismount from the boat
- Cannot escape preview boundaries (configurable distance)
- Cannot teleport away using any teleport commands
- Blocked commands: `/tp`, `/teleport`, `/warp`, `/home`, `/spawn`, `/back`, `/quit`, `/logout`
- Restricted command system with whitelist

#### Interaction Prevention
- Cannot break or place blocks
- Cannot open inventories or interact with blocks
- Cannot interact with entities
- Cannot drop items
- Cannot create portals
- Cannot take damage (protected)

#### Edge Case Handling
- **Disconnects**: Sessions automatically cleaned up
- **Death**: Preview ends and player returns to original location
- **Teleportation Attempts**: Blocked or redirected to preview area
- **World Changes**: Prevented via event handlers
- **Plugin Reloads**: All sessions properly cleaned up
- **Server Restarts**: Clean shutdown of all active sessions
- **Vehicle Removal**: Automatically detected and session ended
- **Bypass Attempts**: Multiple layers of prevention

### 3. Hardened Permission System

Clear, strict permission nodes with proper defaults:

#### Basic Permissions
- `killeffects.use` - Basic plugin access
- `killeffects.gui` - Open the GUI menu
- `killeffects.gui.categories` - Browse effect categories
- `killeffects.gui.favorites` - Manage favorites
- `killeffects.gui.info` - View personal statistics

#### Effect Permissions
- `killeffects.effect.set` - Set personal effect
- `killeffects.effect.use` - Use selected effects
- `killeffects.effect.set.others` - Set effects for other players
- `killeffects.effect.<name>` - Access specific effect
- `killeffects.effect.*` - Access all effects

#### Preview Permissions
- `killeffects.preview.use` - Preview effects
- `killeffects.preview.all` - Preview any effect (bypasses unlock requirement)

#### Admin Permissions
- `killeffects.reload` - Reload configuration
- `killeffects.admin` - Admin access
- `killeffects.admin.see-previews` - See players in preview mode
- `killeffects.* ` - All permissions

#### Permission Features
- Helpful denial messages
- Debug mode shows required permission
- Wildcard support: `killeffects.effect.*` covers all specific effects
- Operator bypass for all permissions
- Clear error messages when permissions are missing

### 4. Configuration

#### Preview Settings (config.yml)
```yaml
preview:
  # Enable or disable the entire preview system (enabled by default)
  enabled: true
  
  # World where previews occur
  world: "world"
  
  # Preview location coordinates (isolated from main gameplay)
  x: 0
  y: 100
  z: 0
  
  # Maximum distance players can move from preview location (blocks)
  max-distance: 10
```

#### Security Settings
```yaml
security:
  # If true, players can only preview effects they have unlocked
  require-preview-unlock: true
```

#### GUI Preview Settings
```yaml
gui:
  preview:
    enabled: true
    duration: 40  # ticks
    location-offset: 3  # blocks from player for effect display
    cooldown: 5  # seconds between previews
```

### 5. Usage

#### Command
```
/killeffects preview <effect>
```

#### GUI
- Middle-click or Shift-click any effect to preview
- Preview automatically handles isolation and restoration
- Cooldown applied between previews

#### Exit Preview
- Type `/killeffects` or any killeffects command to return to normal gameplay
- Automatically exits on disconnect, death, or plugin reload

## Technical Implementation

### Core Classes

#### PreviewSession
- Tracks individual player preview state
- Maintains original location and preview location
- Manages boat entity reference
- Handles session lifecycle

#### PreviewEnvironmentManager
- Manages all active preview sessions
- Handles teleportation to preview area
- Mounts players in boats
- Enforces cooldowns
- Manages player visibility
- Cleans up stale sessions

#### PreviewListener
- Blocks all escape attempts
- Prevents command execution (except whitelist)
- Prevents damage, death, interaction
- Validates boat integrity
- Enforces movement boundaries

#### PermissionManager
- Centralized permission checking
- Helpful denial messages
- Wildcard and inheritance support
- Debug permission display

### Event Handling Priority
- Uses `EventPriority.HIGHEST` for most restrictions
- Uses `EventPriority.MONITOR` for cleanup and validation
- Ensures comprehensive coverage of all player actions

## Configuration Examples

### Server Owner Setup

1. **Enable Preview System** (enabled by default)
   - Leave `preview.enabled: true` in config.yml

2. **Set Preview Location**
   - Choose an isolated world or area
   - Update x, y, z coordinates in config.yml
   - Example: End dimension at high Y coordinate

3. **Set Permissions**
   - Give players `killeffects.preview.use`
   - Give players `killeffects.effect.set` to use effects
   - Give admins `killeffects.admin.see-previews` if desired

### Example Permission Setup (LuckPerms)

```yaml
groups:
  default:
    permissions:
      killeffects.use: true
      killeffects.gui: true
      killeffects.effect.set: true
      killeffects.preview.use: true
  
  vip:
    permissions:
      killeffects.preview.all: true  # Preview any effect
  
  admin:
    permissions:
      killeffects.admin.see-previews: true
```

## Security Notes

- **Player Isolation**: Uses `hidePlayer()` to completely hide players from each other
- **Boat Mounting**: Prevents dismounting via event cancellation
- **Command Blocking**: Whitelist approach prevents exploitation
- **Boundary Enforcement**: Teleports players back if they exceed distance
- **Comprehensive Event Coverage**: Multiple layers prevent all known bypass vectors
- **Session Cleanup**: Proper cleanup on disconnect/reload/restart

## Limitations & Design Decisions

1. **Preview Duration**: Limited by effect duration (configurable)
2. **Single Effect**: One effect per preview (system could be extended)
3. **Cooldown**: Global per-player cooldown between previews
4. **Boat Visibility**: Boat is invisible but still provides collision
5. **Effect Location**: Effect spawns in front of player for visibility

## Future Enhancements

Potential improvements for future versions:
- Multiple effects in one preview session
- Configurable preview duration independently from effect
- Preview area world builder/generator
- Statistics tracking for preview usage
- Per-effect preview cooldowns
- Preview history/replay system
- Custom preview particle displays

## Support

For issues or questions about the preview system:
- Check config.yml is properly formatted
- Verify permissions are set correctly
- Enable debug mode in config.yml
- Check console for error messages
- Ensure preview world exists
