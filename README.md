# LightKillEffects

LightKillEffects is a lightweight Paper plugin that adds configurable visual effects when players get kills. It includes a simple GUI, per-effect permissions, previews, favorites, cooldowns, and persistent player preferences.

Built for servers that want polished kill feedback without heavy dependencies or noisy setup.

## Showcase

- [Showcase Video 1](./showcase1.mp4)
- [Showcase Video 2](./showcase2.mp4)

## Features

- 30 built-in kill effects
- Inventory GUI with categories and favorites
- Per-effect permissions
- Safer preview arena with cooldowns, auto-exit, and command controls
- Player kill and effect usage stats
- Configurable particles, sounds, render distance, cooldowns, categories, effects, and messages
- YAML, JSON, or SQLite player data storage
- Safe player data backups and restricted import/export paths
- Performance mode for busier servers

## Requirements

- Paper or compatible Bukkit server
- Minecraft 1.20.4+
- Java 17+

## Installation

1. Download `lightkilleffects-<version>.jar` from Releases.
2. Put it in your server `plugins` folder.
3. Restart the server.
4. Edit the files in `plugins/LightKillEffects`.
5. Run `/killeffects reload`.

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/killeffects` | `killeffects.use` | Open the menu |
| `/killeffects set <effect>` | `killeffects.use` | Select an effect |
| `/killeffects set none` | `killeffects.use` | Disable your effect |
| `/killeffects preview <effect>` | `killeffects.use` | Preview an effect |
| `/killeffects exit` | `killeffects.use` | Leave an active preview |
| `/killeffects favorite <add/remove> <effect>` | `killeffects.use` | Manage favorites |
| `/killeffects info` | `killeffects.use` | Show your current effect and stats |
| `/killeffects reload` | `killeffects.reload` | Reload config |

Aliases: `/ke`, `/effects`

## Permissions

- `killeffects.use` - basic access
- `killeffects.gui` - open the GUI
- `killeffects.preview` - preview unlocked effects
- `killeffects.reload` - reload config
- `killeffects.set.others` - set effects for other players
- `killeffects.preview.all` - preview any effect
- `killeffects.admin.see-previews` - see players while they are in preview
- `killeffects.use.*` - access all effects
- `killeffects.use.<effect>` - access one effect
- `killeffects.*` - all permissions

## Configuration

Configuration is split so the main file stays readable:

- `config.yml` - behavior, GUI/menu items, performance, storage, and safety settings
- `categories.yml` - category names, icons, and effect lists
- `effects.yml` - effect names, icons, sounds, and descriptions
- `messages.yml` - player-facing text and command help

Player data is kept under `plugins/LightKillEffects/data`. Set `storage.type` to `yaml`, `json`, or `sqlite`.

## Roadmap

Next goal: trials. The plan is to support two effect tracks:

- Kill effects: effects that run after a player gets a kill
- Trial effects: particles that spawn at the player's feet while walking

## Building

```bash
mvn clean package
```

The jar is created in:

```text
target/lightkilleffects-<version>.jar
```
