# CdrVephilimLumberjack

MMORPG-style lumber gathering for Vephilim Roleplay.

## v0.1.4 Core Concept

- Diamond Axe only.
- Admin-defined logging nodes.
- One node represents one whole tree, not one block.
- Connected trunk/branch logs and nearby leaves are captured automatically when the node is created.
- Any registered log in the tree can be used for the same 3-hit chopping progress.
- Registered leaves are protected as part of the tree and cannot be broken through vanilla mechanics.
- One custom resource: `Vephilim Timber`.
- Per-player node cooldowns, not global cooldowns.
- During cooldown, the harvested tree remains visibly solid for that player instead of becoming invisible AIR.
- Logs are shown as stripped variants of the same wood type while keeping their original axis.
- Leaves use a configurable solid harvested material, defaulting to `BROWN_STAINED_GLASS`.
- This avoids ghost-block situations where the player sees empty space but still collides with the real server tree.
- When cooldown ends, the exact original trunk, branches, and leaves are restored for that player.
- Vanilla trees outside registered nodes are unaffected.
- Timber uses PersistentDataContainer tags so it is distinct from vanilla logs.
- Designed to be sold through NPC shops in CdrVephilimEconomy.
- Player-facing feedback is ActionBar only. Lumberjack gameplay does not write status text into chat.
- Player-issued admin command responses also use ActionBar; console output remains normal text.
- Legacy v0.1.x anchor-only nodes are automatically upgraded to whole-tree membership on load.

## Commands

- `/lumber node create <id>` - look at any trunk log; the plugin captures the whole tree under one ID.
- `/lumber node remove <id>` - remove a node.
- `/lumber node list` - list registered nodes.
- `/lumber node info <id>` - inspect a node and captured block count.
- `/lumber timber give <player> [amount]` - give the tagged Timber item, useful for NPC shop setup.
- `/lumber reload` - reload configuration and node data.
- `/lumber status` - show compact runtime status.

## Requirements

- Paper 1.21.11
- Java 21

## Build

```bash
mvn clean package
```

Output: `target/CdrVephilimLumberjack-0.1.4.jar`

## License

MENKIESTES SOFTWARE LICENSE v1.0. MENKIESTES is created by CADERA.
