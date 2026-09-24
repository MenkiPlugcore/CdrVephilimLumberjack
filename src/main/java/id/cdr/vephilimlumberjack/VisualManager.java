package id.cdr.vephilimlumberjack;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class VisualManager {
    private final CdrVephilimLumberjack plugin;
    private final Map<UUID, Map<String, List<Location>>> hiddenViews = new HashMap<>();

    public VisualManager(CdrVephilimLumberjack plugin) {
        this.plugin = plugin;
    }

    public void hideFor(Player player, TreeNode node) {
        if (!plugin.getConfig().getBoolean("visual.enabled", true)) return;
        Block root = node.block();
        if (root == null) return;

        List<Block> visualBlocks = captureTree(root);
        if (visualBlocks.isEmpty()) visualBlocks = List.of(root);

        Material stump = Material.matchMaterial(plugin.getConfig().getString("visual.stump-material", "STRIPPED_OAK_LOG"));
        if (stump == null || !stump.isBlock()) stump = Material.STRIPPED_OAK_LOG;
        BlockData air = Material.AIR.createBlockData();
        BlockData stumpData = stump.createBlockData();

        List<Location> changed = new ArrayList<>();
        for (Block block : visualBlocks) {
            changed.add(block.getLocation());
            if (sameBlock(block, root)) {
                player.sendBlockChange(block.getLocation(), stumpData);
            } else {
                player.sendBlockChange(block.getLocation(), air);
            }
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
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                clearView(player.getUniqueId(), node.id());
                return;
            }
            if (!plugin.cooldowns().isCooling(player.getUniqueId(), node.id())) {
                restoreFor(player, node.id());
            }
        }, ticks);
    }

    public void reapplyActiveCooldowns(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            for (String nodeId : plugin.cooldowns().activeNodes(player.getUniqueId())) {
                TreeNode node = plugin.nodes().get(nodeId);
                if (node == null) continue;
                long remaining = plugin.cooldowns().remainingMillis(player.getUniqueId(), nodeId);
                if (remaining <= 0L) continue;
                hideFor(player, node);
                scheduleRestore(player, node, remaining);
            }
        }, 10L);
    }

    private void clearView(UUID uuid, String nodeId) {
        Map<String, List<Location>> views = hiddenViews.get(uuid);
        if (views == null) return;
        views.remove(nodeId);
        if (views.isEmpty()) hiddenViews.remove(uuid);
    }

    private List<Block> captureTree(Block root) {
        if (!isLog(root.getType())) return List.of(root);

        int scanRadius = Math.max(1, plugin.getConfig().getInt("visual.scan-radius", 7));
        int leafRadius = Math.max(0, plugin.getConfig().getInt("visual.leaf-radius", 2));
        int maxBlocks = Math.max(1, plugin.getConfig().getInt("visual.max-blocks", 160));

        Set<Block> logs = new LinkedHashSet<>();
        Set<Block> visited = new HashSet<>();
        Deque<Block> queue = new ArrayDeque<>();
        queue.add(root);
        visited.add(root);

        while (!queue.isEmpty() && logs.size() < maxBlocks) {
            Block current = queue.removeFirst();
            if (!isLog(current.getType())) continue;
            logs.add(current);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Block next = current.getRelative(dx, dy, dz);
                        if (visited.contains(next)) continue;
                        if (!withinRadius(root, next, scanRadius)) continue;
                        visited.add(next);
                        if (isLog(next.getType())) queue.addLast(next);
                    }
                }
            }
        }

        LinkedHashSet<Block> result = new LinkedHashSet<>(logs);
        if (leafRadius > 0) {
            for (Block log : logs) {
                if (result.size() >= maxBlocks) break;
                for (int dx = -leafRadius; dx <= leafRadius && result.size() < maxBlocks; dx++) {
                    for (int dy = -leafRadius; dy <= leafRadius && result.size() < maxBlocks; dy++) {
                        for (int dz = -leafRadius; dz <= leafRadius && result.size() < maxBlocks; dz++) {
                            Block candidate = log.getRelative(dx, dy, dz);
                            if (!withinRadius(root, candidate, scanRadius + leafRadius)) continue;
                            if (isLeaf(candidate.getType())) result.add(candidate);
                        }
                    }
                }
            }
        }
        return new ArrayList<>(result);
    }

    private boolean withinRadius(Block root, Block other, int radius) {
        return Math.abs(root.getX() - other.getX()) <= radius
                && Math.abs(root.getY() - other.getY()) <= radius
                && Math.abs(root.getZ() - other.getZ()) <= radius;
    }

    private boolean isLog(Material material) {
        return Tag.LOGS.isTagged(material);
    }

    private boolean isLeaf(Material material) {
        return Tag.LEAVES.isTagged(material);
    }

    private boolean sameBlock(Block a, Block b) {
        return a.getWorld().equals(b.getWorld())
                && a.getX() == b.getX()
                && a.getY() == b.getY()
                && a.getZ() == b.getZ();
    }
}
