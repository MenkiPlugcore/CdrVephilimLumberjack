# CdrVephilimLumberjack

MMORPG-style lumber gathering for Vephilim Roleplay.

## v0.1.2 Core Concept

- Diamond Axe only.
- Admin-defined logging nodes.
- One node represents one whole tree, not one block.
- Connected trunk/branch logs and nearby leaves are captured automatically when the node is created.
- Any registered log in the tree can be used for the same 3-hit chopping progress.
- Registered leaves are protected as part of the tree and cannot be broken through vanilla mechanics.
- One custom resource: `Vephilim Timber`.
- Per-player node cooldowns, not global cooldowns.
- Per-player harvested-tree visuals hide the full captured tree, including leaves, while leaving a stump.
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

Output: `target/CdrVephilimLumberjack-0.1.2.jar`

## License

MENKIESTES SOFTWARE LICENSE v1.0. MENKIESTES is created by CADERA.
