# Changelog

## 0.2.1

Configurable chopping proximity patch.

- Added a server-side chop range check for registered lumber nodes.
- Chopping only progresses when the player is within the configured distance of any registered trunk block in that tree.
- Range validation applies to both left-click and right-click chopping.
- Being too far does not consume durability, increment hit progress, start cooldown, or award Timber.
- Added `chop-range.enabled` and `chop-range.max-distance` configuration options.
- Default maximum chopping distance is 3.5 blocks.
- Setting `max-distance` to `0` or disabling the range section turns the proximity restriction off.
- Out-of-range feedback remains ActionBar-only.

## 0.2.0

Dedicated Timber seller NPC system.

- Added Citizens NPC integration dedicated to selling `Vephilim Timber`.
- Seller NPC IDs are configured directly in `config.yml`.
- Each NPC can have its own configurable `price-per-timber`.
- Right-clicking a configured seller NPC sells all tagged Vephilim Timber from the player's storage inventory.
- Vanilla logs and renamed lookalikes are rejected because selling validates the Timber PersistentDataContainer tag.
- Payments are deposited through Vault's active Economy provider.
- Sale feedback remains ActionBar-only to keep player chat clean.
- Added interaction throttling to prevent repeated double transactions.
- Failed Vault deposits automatically return the removed Timber to the player, dropping overflow safely if inventory is full.
- Citizens and Vault are now required runtime dependencies for the seller system.

## 0.1.5

Dual-click chopping input patch.

- Registered lumber logs can now be chopped with either left click or right click.
- Left-click and right-click hits share the same per-player 3-hit progress state.
- Existing hit delay, durability cost, ActionBar feedback, cooldown, and reward rules apply identically to both inputs.
- Right-click interactions on registered tree blocks are cancelled so Diamond Axes cannot strip the real server log or trigger vanilla block interaction side effects.
- Off-hand interaction events remain ignored to prevent duplicate right-click hits.

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
