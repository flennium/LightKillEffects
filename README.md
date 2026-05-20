# LightKillEffects

LightKillEffects is a lightweight Paper plugin that adds configurable visual effects when players get kills. It includes a simple GUI, per-effect permissions, previews, favorites, cooldowns, and persistent player preferences.

Built for servers that want polished kill feedback without heavy dependencies or noisy setup.

## Features

- 20 built-in kill effects
- Inventory GUI with categories and favorites
- Per-effect permissions
- Optional previews with cooldowns
- Player kill and effect usage stats
- Configurable particles, sounds, render distance, cooldowns, and messages
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
4. Edit `plugins/LightKillEffects/config.yml`.
5. Run `/killeffects reload`.

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/killeffects` | `killeffects.use` | Open the menu |
| `/killeffects set <effect>` | `killeffects.use` | Select an effect |
| `/killeffects set none` | `killeffects.use` | Disable your effect |
| `/killeffects preview <effect>` | `killeffects.use` | Preview an effect |
| `/killeffects favorite <add/remove> <effect>` | `killeffects.use` | Manage favorites |
| `/killeffects info` | `killeffects.use` | Show your current effect and stats |
| `/killeffects reload` | `killeffects.reload` | Reload config |

Aliases: `/ke`, `/effects`

## Permissions

- `killeffects.use` - basic access
- `killeffects.gui` - open the GUI
- `killeffects.reload` - reload config
- `killeffects.set.others` - set effects for other players
- `killeffects.preview.all` - preview any effect
- `killeffects.use.*` - access all effects
- `killeffects.use.<effect>` - access one effect
- `killeffects.*` - all permissions

## Configuration

Most behavior is controlled in `config.yml`.

Useful sections:

- `general` - enable state, defaults, cooldowns, notifications
- `security` - mob kill effects, console control, preview unlock checks
- `performance` - particle limits, render distance, cleanup interval
- `storage` - backup limits and import/export safety
- `gui` - title, sounds, click cooldowns, preview settings
- `effects` - display names, icons, sounds, descriptions
- `messages` - all player-facing text

## Building

```bash
mvn clean package
```

The jar is created in:

```text
target/lightkilleffects-<version>.jar
```

## Versioning

LightKillEffects uses semantic versioning.

- Patch: bug fixes, for example `1.1.1`
- Minor: compatible features/config additions, for example `1.2.0`
- Major: breaking changes, for example `2.0.0`

Tags use `v<version>`, such as `v1.1.0`.
