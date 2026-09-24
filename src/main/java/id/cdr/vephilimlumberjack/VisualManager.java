package id.cdr.vephilimlumberjack;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
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

        Block stumpBlock = findLowestLog(visualBlocks, root);

        Material stump = Material.matchMaterial(plugin.getConfig().getString("visual.stump-material", "STRIPPED_OAK_LOG"));
        if (stump == null || !stump.isBlock()) stump = Material.STRIPPED_OAK_LOG;
        BlockData air = Material.AIR.createBlockData();
        BlockData stumpData = stump.createBlockData();

        List<Location> changed = new ArrayList<>();
        for (Block block : visualBlocks) {
            changed.add(block.getLocation());
            if (stumpBlock != null && sameBlock(block, stumpBlock)) {
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

    private Block findLowestLog(List<Block> blocks, Block fallback) {
        Block lowest = null;
        for (Block block : blocks) {
            if (!Tag.LOGS.isTagged(block.getType())) continue;
            if (lowest == null || block.getY() < lowest.getY()) lowest = block;
        }
        return lowest == null ? fallback : lowest;
    }

    private boolean sameBlock(Block a, Block b) {
        return a.getWorld().equals(b.getWorld())
                && a.getX() == b.getX()
                && a.getY() == b.getY()
                && a.getZ() == b.getZ();
    }
}
