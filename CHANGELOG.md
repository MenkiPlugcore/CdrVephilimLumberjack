# Changelog

## 0.1.4

Solid harvested-tree visual patch.

- Replaced invisible AIR-based harvested visuals with a solid dead-tree state.
- Registered logs now appear as the stripped variant of the same wood type while the node is cooling down.
- Original log axis is preserved so trunks and branches keep their orientation.
- Registered leaves now use a configurable solid harvested material, defaulting to `BROWN_STAINED_GLASS`.
- Unexpected captured blocks are kept visually identical to the live world instead of being hidden.
- Removed the misleading ghost-block situation where a player could see empty space but still collide with the real server block.
- Harvested visuals remain per-player and restore to the exact original tree when cooldown ends.

## 0.1.3

Tree visual restore reliability patch.

- Fixed harvested trees sometimes remaining visually chopped after their cooldown had already expired.
- Restore tasks now re-check the exact remaining cooldown and automatically reschedule if they fire slightly early.
- Once cooldown reaches zero, the player's client is force-restored to the live original tree blocks.
- Added an interaction safety net that restores any stale client-side stump state before a ready node can be harvested again.
- Whole-tree restoration includes the original trunk, branches, and leaves captured by the node.

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
