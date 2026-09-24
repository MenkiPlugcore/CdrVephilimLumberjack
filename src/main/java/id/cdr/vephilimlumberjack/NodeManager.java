package id.cdr.vephilimlumberjack;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class NodeManager {
    private final CdrVephilimLumberjack plugin;
    private final File file;
    private final Map<String, TreeNode> nodes = new LinkedHashMap<>();
    private final Map<String, List<BlockKey>> nodeBlocks = new HashMap<>();
    private final Map<BlockKey, TreeNode> blockIndex = new HashMap<>();

    public NodeManager(CdrVephilimLumberjack plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "nodes.yml");
    }

    public void load() {
        nodes.clear();
        nodeBlocks.clear();
        blockIndex.clear();

        if (!file.exists()) {
            save();
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("nodes");
        if (section == null) return;

        boolean migratedLegacyNode = false;

        for (String key : section.getKeys(false)) {
            String id = key.toLowerCase(Locale.ROOT);
            String base = "nodes." + key + ".";
            String world = yaml.getString(base + "world");
            if (world == null || world.isBlank()) continue;

            TreeNode node = new TreeNode(
                    id,
                    world,
                    yaml.getInt(base + "x"),
                    yaml.getInt(base + "y"),
                    yaml.getInt(base + "z")
            );
            nodes.put(id, node);

            List<BlockKey> members = new ArrayList<>();
            for (String stored : yaml.getStringList(base + "blocks")) {
                BlockKey parsed = parseKey(world, stored);
                if (parsed != null) members.add(parsed);
            }

            if (members.isEmpty()) {
                Block anchor = node.block();
                if (anchor != null && isLog(anchor.getType())) {
                    members.addAll(captureTree(anchor));
                }
                if (members.isEmpty()) {
                    members.add(new BlockKey(world, node.x(), node.y(), node.z()));
                }
                migratedLegacyNode = true;
            }

            registerMembership(node, members);
        }

        if (migratedLegacyNode) save();
    }

    public void save() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        YamlConfiguration yaml = new YamlConfiguration();

        for (TreeNode node : nodes.values()) {
            String base = "nodes." + node.id() + ".";
            yaml.set(base + "world", node.worldName());
            yaml.set(base + "x", node.x());
            yaml.set(base + "y", node.y());
            yaml.set(base + "z", node.z());

            List<String> serialized = new ArrayList<>();
            for (BlockKey key : nodeBlocks.getOrDefault(node.id(), List.of())) {
                serialized.add(key.x() + "," + key.y() + "," + key.z());
            }
            yaml.set(base + "blocks", serialized);
        }

        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save nodes.yml: " + ex.getMessage());
        }
    }

    public TreeNode create(String rawId, Block block) {
        String id = rawId.toLowerCase(Locale.ROOT);
        TreeNode node = new TreeNode(id, block.getWorld().getName(), block.getX(), block.getY(), block.getZ());

        unregisterMembership(id);
        nodes.put(id, node);

        List<BlockKey> captured = captureTree(block);
        if (captured.isEmpty()) {
            captured = List.of(BlockKey.of(block));
        }
        registerMembership(node, captured);
        save();
        return node;
    }

    public boolean remove(String rawId) {
        String id = rawId.toLowerCase(Locale.ROOT);
        TreeNode removedNode = nodes.remove(id);
        if (removedNode == null) return false;
        unregisterMembership(id);
        save();
        return true;
    }

    public TreeNode get(String rawId) {
        return nodes.get(rawId.toLowerCase(Locale.ROOT));
    }

    public TreeNode getByBlock(Block block) {
        return blockIndex.get(BlockKey.of(block));
    }

    public List<Block> blocks(TreeNode node) {
        World world = node.block() == null ? null : node.block().getWorld();
        if (world == null) return List.of();

        List<Block> result = new ArrayList<>();
        for (BlockKey key : nodeBlocks.getOrDefault(node.id(), List.of())) {
            if (!key.worldName().equals(world.getName())) continue;
            result.add(world.getBlockAt(key.x(), key.y(), key.z()));
        }
        return result;
    }

    public int blockCount(TreeNode node) {
        return nodeBlocks.getOrDefault(node.id(), List.of()).size();
    }

    public Collection<TreeNode> all() {
        return nodes.values();
    }

    public int size() {
        return nodes.size();
    }

    private void registerMembership(TreeNode node, Collection<BlockKey> requested) {
        List<BlockKey> accepted = new ArrayList<>();
        for (BlockKey key : requested) {
            TreeNode existing = blockIndex.get(key);
            if (existing != null && !existing.id().equals(node.id())) continue;
            blockIndex.put(key, node);
            accepted.add(key);
        }
        nodeBlocks.put(node.id(), accepted);
    }

    private void unregisterMembership(String nodeId) {
        List<BlockKey> previous = nodeBlocks.remove(nodeId);
        if (previous == null) return;
        for (BlockKey key : previous) {
            TreeNode owner = blockIndex.get(key);
            if (owner != null && owner.id().equals(nodeId)) blockIndex.remove(key);
        }
    }

    private List<BlockKey> captureTree(Block root) {
        if (!isLog(root.getType())) return List.of();

        int scanRadius = Math.max(1, plugin.getConfig().getInt("visual.scan-radius", 7));
        int leafRadius = Math.max(0, plugin.getConfig().getInt("visual.leaf-radius", 2));
        int maxBlocks = Math.max(1, plugin.getConfig().getInt("visual.max-blocks", 160));

        Set<BlockKey> visited = new HashSet<>();
        LinkedHashSet<BlockKey> logs = new LinkedHashSet<>();
        Deque<Block> queue = new ArrayDeque<>();

        queue.add(root);
        visited.add(BlockKey.of(root));

        while (!queue.isEmpty() && logs.size() < maxBlocks) {
            Block current = queue.removeFirst();
            if (!isLog(current.getType())) continue;
            logs.add(BlockKey.of(current));

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Block next = current.getRelative(dx, dy, dz);
                        BlockKey nextKey = BlockKey.of(next);
                        if (visited.contains(nextKey)) continue;
                        if (!withinRadius(root, next, scanRadius)) continue;
                        visited.add(nextKey);
                        if (isLog(next.getType())) queue.addLast(next);
                    }
                }
            }
        }

        LinkedHashSet<BlockKey> result = new LinkedHashSet<>(logs);
        if (leafRadius > 0 && result.size() < maxBlocks) {
            for (BlockKey logKey : logs) {
                if (result.size() >= maxBlocks) break;
                World world = root.getWorld();
                Block log = world.getBlockAt(logKey.x(), logKey.y(), logKey.z());

                for (int dx = -leafRadius; dx <= leafRadius && result.size() < maxBlocks; dx++) {
                    for (int dy = -leafRadius; dy <= leafRadius && result.size() < maxBlocks; dy++) {
                        for (int dz = -leafRadius; dz <= leafRadius && result.size() < maxBlocks; dz++) {
                            Block candidate = log.getRelative(dx, dy, dz);
                            if (!withinRadius(root, candidate, scanRadius + leafRadius)) continue;
                            if (isLeaf(candidate.getType())) result.add(BlockKey.of(candidate));
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

    private BlockKey parseKey(String worldName, String serialized) {
        String[] parts = serialized.split(",");
        if (parts.length != 3) return null;
        try {
            return new BlockKey(
                    worldName,
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())
            );
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record BlockKey(String worldName, int x, int y, int z) {
        private static BlockKey of(Block block) {
            return new BlockKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        }
    }
}
