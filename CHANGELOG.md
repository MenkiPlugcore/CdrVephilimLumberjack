# Changelog

## 0.1.1

ActionBar-only player UI patch.

- Routed all lumber gameplay feedback to ActionBar only.
- Routed player-issued admin command feedback to ActionBar instead of chat.
- Kept console command output as normal text because console has no ActionBar.
- Consolidated multi-line command status into compact single-line ActionBar output.
- No lumber gameplay messages are written into player chat.

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
