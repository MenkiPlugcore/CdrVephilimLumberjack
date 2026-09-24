# CdrVephilimLumberjack

MMORPG-style lumber gathering for Vephilim Roleplay.

## v0.1.0 Core Concept

- Diamond Axe only.
- Admin-defined logging nodes.
- 3 valid hits per harvest by default.
- One custom resource: `Vephilim Timber`.
- Per-player node cooldowns, not global cooldowns.
- Per-player harvested-tree visuals using client-side block changes.
- Vanilla trees outside registered nodes are unaffected.
- Timber uses PersistentDataContainer tags so it is distinct from vanilla logs.
- Designed to be sold through NPC shops in CdrVephilimEconomy.

## Commands

- `/lumber node create <id>` - create a node from the log block you are looking at.
- `/lumber node remove <id>` - remove a node.
- `/lumber node list` - list registered nodes.
- `/lumber node info <id>` - inspect a node.
- `/lumber timber give <player> [amount]` - give the tagged Timber item, useful for NPC shop setup.
- `/lumber reload` - reload configuration and node data.

## Requirements

- Paper 1.21.11
- Java 21

## Build

```bash
mvn clean package
```

Output: `target/CdrVephilimLumberjack-0.1.0.jar`

## License

MENKIESTES SOFTWARE LICENSE v1.0. MENKIESTES is created by CADERA.
