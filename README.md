# NeDisguise

A powerful and highly customizable disguise plugin for Minecraft/Spigot, with full LuckPerms and MySQL support.

## Features
- **GUI-Based Disguise:** Easy-to-use GUI for selecting ranks and skins.
- **LuckPerms Integration:** Automatically fetches prefixes and suffixes from LuckPerms for dynamic rank displays.
- **Custom Names:** Allows players to input custom names for their disguise.
- **Multi-Server Sync:** Optional MySQL support to sync disguise data across multiple servers. Falls back to YAML if disabled.
- **Fully Configurable:** Almost everything is customizable in `config.yml` including items, GUI sizes, messages, colors, sounds, and rank mappings.
- **Customizable Commands:** Define aliases for the disguise command directly in the config.

## Requirements
- Java 8+
- Spigot 1.8.8+ (API version 1.13 specified in plugin.yml, but compiled against 1.8.8)
- [LuckPerms](https://luckperms.net/)

## Commands & Permissions
- `/disguise` (configurable) - Opens the disguise GUI.
- Permission: `disguise.perm` (configurable in `config.yml`)

## Installation
1. Download the latest release from the [Releases page](../../releases).
2. Drop the `NeDisguise-1.0-SNAPSHOT.jar` into your server's `plugins` folder.
3. Make sure LuckPerms is installed.
4. Restart the server.
5. Configure the plugin in `plugins/NeDisguise/config.yml`.

## Configuration
See the default `config.yml` in the repository for all available options.

## License
MIT License. See [LICENSE](LICENSE) for more information.
