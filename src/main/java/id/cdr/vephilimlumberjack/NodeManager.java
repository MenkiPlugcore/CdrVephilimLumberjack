package id.cdr.vephilimlumberjack;

import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class NodeManager {
    private final CdrVephilimLumberjack plugin;
    private final File file;
    private final Map<String, TreeNode> nodes = new LinkedHashMap<>();

    public NodeManager(CdrVephilimLumberjack plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "nodes.yml");
    }

    public void load() {
        nodes.clear();
        if (!file.exists()) {
            save();
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("nodes");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String base = "nodes." + key + ".";
            String world = yaml.getString(base + "world");
            if (world == null || world.isBlank()) continue;
            nodes.put(key.toLowerCase(Locale.ROOT), new TreeNode(
                    key.toLowerCase(Locale.ROOT),
                    world,
                    yaml.getInt(base + "x"),
                    yaml.getInt(base + "y"),
                    yaml.getInt(base + "z")
            ));
        }
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
        nodes.put(id, node);
        save();
        return node;
    }

    public boolean remove(String rawId) {
        boolean removed = nodes.remove(rawId.toLowerCase(Locale.ROOT)) != null;
        if (removed) save();
        return removed;
    }

    public TreeNode get(String rawId) {
        return nodes.get(rawId.toLowerCase(Locale.ROOT));
    }

    public TreeNode getByBlock(Block block) {
        for (TreeNode node : nodes.values()) {
            if (node.worldName().equals(block.getWorld().getName())
                    && node.x() == block.getX()
                    && node.y() == block.getY()
                    && node.z() == block.getZ()) {
                return node;
            }
        }
        return null;
    }

    public Collection<TreeNode> all() {
        return nodes.values();
    }

    public int size() {
        return nodes.size();
    }
}
