package id.cdr.vephilimlumberjack;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.Axis;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VisualManager {
    private final CdrVephilimLumberjack plugin;
    private final Map<UUID, Map<String, List<Location>>> hiddenViews = new HashMap<>();

    public VisualManager(CdrVephilimLumberjack plugin) {
        this.plugin = plugin;
    }

    public void hideFor(Player player, TreeNode node) {
        if (!plugin.getConfig().getBoolean("visual.enabled", true)) return;

        List<Block> visualBlocks = plugin.nodes().blocks(node);
        Block root = node.block();
        if (visualBlocks.isEmpty() && root != null) visualBlocks = List.of(root);
        if (visualBlocks.isEmpty()) return;

        Material deadLeaf = resolveDeadLeafMaterial();
        BlockData deadLeafData = deadLeaf.createBlockData();

        List<Location> changed = new ArrayList<>();
        for (Block block : visualBlocks) {
            changed.add(block.getLocation());

            if (Tag.LOGS.isTagged(block.getType())) {
                player.sendBlockChange(block.getLocation(), harvestedLogData(block));
                continue;
            }

            if (Tag.LEAVES.isTagged(block.getType())) {
                player.sendBlockChange(block.getLocation(), deadLeafData);
                continue;
            }

            // Keep any unexpected captured block visually identical to the real world.
            // Never fake AIR here because server-side collision still exists.
            player.sendBlockChange(block.getLocation(), block.getBlockData());
        }

        hiddenViews.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>())
                .put(node.id(), changed);
    }

    public void restoreFor(Player player, String nodeId) {
        Map<String, List<Location>> playerViews = hiddenViews.get(player.getUniqueId());
        if (playerViews == null) return;
        List<Location> changed = playerViews.remove(nodeId);
        if (changed == null) return;

        for (Location location : changed) {
            if (location.getWorld() == null) continue;
            Block live = location.getBlock();
            player.sendBlockChange(location, live.getBlockData());
        }
        if (playerViews.isEmpty()) hiddenViews.remove(player.getUniqueId());
    }

    public void restoreIfReady(Player player, TreeNode node) {
        if (plugin.cooldowns().remainingMillis(player.getUniqueId(), node.id()) <= 0L) {
            restoreFor(player, node.id());
        }
    }

    public void restoreAll(Player player) {
        Map<String, List<Location>> playerViews = hiddenViews.remove(player.getUniqueId());
        if (playerViews == null) return;
        for (List<Location> locations : playerViews.values()) {
            for (Location location : locations) {
                if (location.getWorld() != null) {
                    player.sendBlockChange(location, location.getBlock().getBlockData());
                }
            }
        }
    }

    public void scheduleRestore(Player player, TreeNode node, long delayMillis) {
        long ticks = Math.max(1L, (delayMillis + 49L) / 50L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> checkAndRestore(player, node), ticks);
    }

    private void checkAndRestore(Player player, TreeNode node) {
        if (!player.isOnline()) {
            clearView(player.getUniqueId(), node.id());
            return;
        }

        long remaining = plugin.cooldowns().remainingMillis(player.getUniqueId(), node.id());
        if (remaining <= 0L) {
            restoreFor(player, node.id());
            return;
        }

        // A Bukkit tick can run a few milliseconds before the wall-clock cooldown expires.
        // Reschedule for the exact remaining time so the harvested visual can never remain stale.
        scheduleRestore(player, node, remaining);
    }

    public void reapplyActiveCooldowns(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            for (String nodeId : plugin.cooldowns().activeNodes(player.getUniqueId())) {
                TreeNode node = plugin.nodes().get(nodeId);
                if (node == null) continue;
                long remaining = plugin.cooldowns().remainingMillis(player.getUniqueId(), nodeId);
                if (remaining <= 0L) {
                    restoreFor(player, nodeId);
                    continue;
                }
                hideFor(player, node);
                scheduleRestore(player, node, remaining);
            }
        }, 10L);
    }

    private BlockData harvestedLogData(Block block) {
        Material original = block.getType();
        Material stripped = strippedVariant(original);
        if (stripped == null || !stripped.isBlock() || !stripped.isSolid()) {
            stripped = resolveFallbackLogMaterial();
        }

        BlockData replacement = stripped.createBlockData();
        BlockData originalData = block.getBlockData();

        if (originalData instanceof Orientable originalOrientable
                && replacement instanceof Orientable replacementOrientable) {
            Axis axis = originalOrientable.getAxis();
            if (replacementOrientable.getAxes().contains(axis)) {
                replacementOrientable.setAxis(axis);
            }
        }
        return replacement;
    }

    private Material strippedVariant(Material original) {
        String name = original.name();
        if (name.startsWith("STRIPPED_")) return original;

        String strippedName = null;
        if (name.endsWith("_LOG")) {
            strippedName = "STRIPPED_" + name;
        } else if (name.endsWith("_WOOD")) {
            strippedName = "STRIPPED_" + name;
        } else if (name.endsWith("_STEM")) {
            strippedName = "STRIPPED_" + name;
        } else if (name.endsWith("_HYPHAE")) {
            strippedName = "STRIPPED_" + name;
        } else if (name.equals("BAMBOO_BLOCK")) {
            strippedName = "STRIPPED_BAMBOO_BLOCK";
        }

        return strippedName == null ? null : Material.matchMaterial(strippedName);
    }

    private Material resolveFallbackLogMaterial() {
        Material material = Material.matchMaterial(plugin.getConfig().getString(
                "visual.harvested-fallback-log-material", "STRIPPED_OAK_LOG"));
        if (material == null || !material.isBlock() || !material.isSolid()) {
            return Material.STRIPPED_OAK_LOG;
        }
        return material;
    }

    private Material resolveDeadLeafMaterial() {
        Material material = Material.matchMaterial(plugin.getConfig().getString(
                "visual.harvested-leaf-material", "BROWN_STAINED_GLASS"));
        if (material == null || !material.isBlock() || !material.isSolid()) {
            return Material.BROWN_STAINED_GLASS;
        }
        return material;
    }

    private void clearView(UUID uuid, String nodeId) {
        Map<String, List<Location>> views = hiddenViews.get(uuid);
        if (views == null) return;
        views.remove(nodeId);
        if (views.isEmpty()) hiddenViews.remove(uuid);
    }
}
