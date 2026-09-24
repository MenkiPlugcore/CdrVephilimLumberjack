# Changelog

## 0.1.2

Whole-tree node patch.

- One registered node now represents the full tree instead of only the selected anchor log.
- Connected logs and nearby leaves are captured and stored as node members.
- Existing legacy anchor-only nodes are automatically migrated on load.
- Any registered trunk/canopy block is protected from vanilla breaking.
- Chopping progress can be triggered from any registered log in the same tree.
- Leaves are protected as part of the node but do not count as chopping hits.
- Per-player harvested visuals now hide the entire captured tree, including leaves, while keeping one stump.
- Admin node creation/info feedback now shows the captured whole-tree block count.

## 0.1.1

ActionBar-only player UI patch.

- Routed all player-facing Lumberjack gameplay feedback to ActionBar.
- Routed player-issued admin command responses to ActionBar.
- Console output remains standard text because console has no ActionBar.

## 0.1.0

Initial core release.

- Added admin-defined lumber nodes.
- Added Diamond Axe-only harvesting.
- Added configurable 3-hit harvesting flow.
- Added per-player hit progress and anti-spam delay.
- Added per-player node cooldowns with persistent cooldown storage.
- Added per-player harvested-tree visuals using client-side block changes.
- Added Vephilim Timber with PersistentDataContainer identity tags.
- Added configurable durability cost per successful hit.
- Added admin commands for node management and Timber item distribution.
- Added CdrVephilimEconomy soft dependency for future integration.
