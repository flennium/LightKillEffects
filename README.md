# LightKillEffects

A stunning Minecraft plugin featuring 20 unique light-based kill effects with beautiful particle animations, comprehensive GUI system, and advanced player management.

## ✨ Features

### 🎆 20 Stunning Kill Effects
- **Lightning Storm** - Multiple lightning bolts in circular patterns
- **Solar Explosion** - Golden sphere with radiating light rays  
- **Frozen Burst** - Ice-blue spiraling particles with shattering ice
- **Neon Rave** - Rapidly cycling colored lights in disco fashion
- **Starfall Cascade** - Glowing particles falling like shooting stars
- **Prismatic Shatter** - Rainbow glass particles with beacon
- **Void Consumption** - Dark purple spiral black hole effect
- **Phoenix Rebirth** - Flame particles in wing patterns
- **Celestial Gateway** - Rotating light portal
- **Electric Overload** - Blue electrical jumping particles
- **Crystal Garden** - Sprouting glowing crystal blocks
- **Spectral Haunt** - Ghostly victim silhouette
- **Laser Light Show** - Intersecting colored laser beams
- **Meteor Impact** - Fiery crater creation
- **Bioluminescent Bloom** - Gentle spreading spore-like glow
- **Time Fracture** - Reality-cracking effect
- **Divine Ascension** - Golden double helix spiral
- **Toxic Meltdown** - Bubbling green corrosive spread
- **Quantum Collapse** - Phasing victim effect
- **Supernova** - Intense white light explosion

### 🎮 Advanced Features
- **Beautiful GUI System** - Inventory-based interface with categories and pagination
- **Permission System** - Granular permissions for each effect
- **Player Data Management** - Persistent storage of preferences and statistics
- **Favorites System** - Save your preferred effects for quick access
- **Effect Previews** - Test effects before selecting them
- **Performance Optimization** - Configurable particle limits and render distances
- **Sound Integration** - Complementary audio effects for each animation
- **Statistics Tracking** - Track kills and effect usage per player
- **Cooldown System** - Prevent effect spam with configurable cooldowns

### 🛠️ Technical Features
- **Smart Caching** - Efficient memory management for large servers
- **Configuration System** - Extensive customization options
- **Debug Mode** - Development and troubleshooting tools
- **Auto-backup** - Automatic player data backups
- **Import/Export** - Data migration tools
- **Performance Mode** - Reduced particles for better server performance

## 📋 Requirements

- **Minecraft Version**: 1.20.4+
- **Server Software**: Paper, Spigot, or Bukkit
- **Java Version**: 17+
- **Memory**: Recommended 2GB+ for optimal performance

## 🚀 Installation

1. Download the latest `lightkilleffects-1.0.0.jar` from the releases
2. Place the JAR file in your server's `plugins` folder
3. Start or restart your server
4. Configure the plugin using `/killeffects reload` after editing config files

## ⚙️ Configuration

### Basic Setup
The plugin creates three configuration files:
- `config.yml` - Main plugin settings and effect configurations
- `plugin.yml` - Permissions and command definitions
- `playerdata.yml` - Player preferences and statistics storage

### Key Configuration Options
```yaml
general:
  default-effect: 'prismatic_shatter'  # Default effect for new players
  global-cooldown: 3                   # Cooldown between effects (seconds)
  max-favorites: 5                     # Maximum favorite effects per player
  
performance:
  max-particles-per-effect: 500        # Particle limit per effect
  particle-render-distance: 32         # Render distance in blocks
  performance-mode: false              # Enable for reduced particles
```

## 🎯 Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/killeffects` | `killeffects.use` | Open the main effects GUI |
| `/killeffects gui` | `killeffects.gui` | Open the effects menu |
| `/killeffects set <effect>` | `killeffects.use` | Set your kill effect |
| `/killeffects set none` | `killeffects.use` | Disable your kill effect |
| `/killeffects preview <effect>` | `killeffects.use` | Preview an effect |
| `/killeffects favorite <add\|remove> <effect>` | `killeffects.use` | Manage favorites |
| `/killeffects info` | `killeffects.use` | View your statistics |
| `/killeffects stats` | `killeffects.use` | Detailed effect statistics |
| `/killeffects reload` | `killeffects.admin` | Reload plugin configuration |

## 🔐 Permissions

### Main Permissions
- `killeffects.*` - Access to all effects and admin functions
- `killeffects.use` - Basic permission to use kill effects
- `killeffects.admin` - Administrative functions
- `killeffects.use.*` - Access to all effects

### Individual Effect Permissions
Each effect has its own permission node:
- `killeffects.use.lightning_storm`
- `killeffects.use.solar_explosion`
- `killeffects.use.frozen_burst`
- And so on for all 20 effects...

### Utility Permissions
- `killeffects.gui` - Access to GUI system
- `killeffects.reload` - Reload configurations
- `killeffects.set.others` - Set effects for other players
- `killeffects.preview.all` - Preview any effect

## 🎨 Effect Categories

Effects are organized into themed categories:

**🔥 Fire Effects**
- Solar Explosion, Phoenix Rebirth, Meteor Impact

**❄️ Ice Effects** 
- Frozen Burst

**⚡ Electric Effects**
- Lightning Storm, Electric Overload

**🔮 Mystical Effects**
- Void Consumption, Celestial Gateway, Spectral Haunt, Time Fracture, Divine Ascension, Quantum Collapse

**🌿 Nature Effects**
- Crystal Garden, Bioluminescent Bloom, Toxic Meltdown

**🌌 Cosmic Effects**
- Starfall Cascade, Supernova

**🎉 Party Effects**
- Neon Rave, Prismatic Shatter, Laser Light Show

## 📊 Performance Optimization

The plugin includes several performance features:

- **Particle Limits** - Configurable maximum particles per effect
- **Render Distance** - Only show effects to nearby players
- **Performance Mode** - Reduced particle counts for better performance
- **Cleanup Tasks** - Automatic cleanup of temporary effects and cooldowns
- **Memory Management** - Optional player cache clearing
- **Effect Optimization** - Optimized animation loops and particle spawning

## 🔧 Advanced Features

### Player Data Management
- Persistent storage of player preferences
- Kill statistics and effect usage tracking
- Automatic data migration and backups
- Import/export functionality for server transfers

### GUI System
- Beautiful inventory-based interface
- Category-based effect browsing
- Pagination for large effect collections
- Preview system for testing effects
- Favorites management
- Real-time permission checking

### Administrative Tools
- Configuration reload without restart
- Player data management commands
- Debug mode for troubleshooting
- Performance monitoring
- Backup and restore functionality

## 🐛 Troubleshooting

### Common Issues

**Effects not working:**
- Check player has `killeffects.use` permission
- Verify effect-specific permissions
- Ensure plugin is enabled: `/killeffects info`

**Performance issues:**
- Enable performance mode in config
- Reduce `max-particles-per-effect`
- Lower `particle-render-distance`
- Enable `clear-cache-on-quit`

**GUI not opening:**
- Check `killeffects.gui` permission
- Verify inventory space
- Check console for errors

### Debug Mode
Enable debug mode in `config.yml` to see detailed logging:
```yaml
general:
  debug: true
```

## 📈 Statistics & Analytics

The plugin tracks comprehensive statistics:
- Total kills per player
- Usage count per effect
- Favorite effects
- Last activity timestamps
- Effect popularity rankings

Access statistics via:
- `/killeffects info` - Personal statistics
- `/killeffects stats` - Detailed usage statistics
- GUI statistics panels

## 🤝 Support

For support, bug reports, or feature requests:
- Create an issue on GitHub
- Join our Discord community
- Check the wiki for detailed documentation

## 📜 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🏆 Credits

Created with ❤️ by flennn

Special thanks to the Minecraft modding community and all contributors who made this project possible.

---

**Bring stunning visual flair to your Minecraft server with LightKillEffects!** ✨