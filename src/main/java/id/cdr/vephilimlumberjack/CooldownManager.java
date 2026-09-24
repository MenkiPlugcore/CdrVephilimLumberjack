package id.cdr.vephilimlumberjack;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CooldownManager {
    private final CdrVephilimLumberjack plugin;
    private final File file;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public CooldownManager(CdrVephilimLumberjack plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "cooldowns.yml");
    }

    public void load() {
        cooldowns.clear();
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("cooldowns");
        if (root == null) return;

        long now = System.currentTimeMillis();
        for (String uuidText : root.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidText);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            ConfigurationSection playerSection = root.getConfigurationSection(uuidText);
            if (playerSection == null) continue;
            Map<String, Long> playerCooldowns = new HashMap<>();
            for (String nodeId : playerSection.getKeys(false)) {
                long expiresAt = playerSection.getLong(nodeId);
                if (expiresAt > now) playerCooldowns.put(nodeId, expiresAt);
            }
            if (!playerCooldowns.isEmpty()) cooldowns.put(uuid, playerCooldowns);
        }
    }

    public void save() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        pruneExpired();
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, Long>> playerEntry : cooldowns.entrySet()) {
            for (Map.Entry<String, Long> nodeEntry : playerEntry.getValue().entrySet()) {
                yaml.set("cooldowns." + playerEntry.getKey() + "." + nodeEntry.getKey(), nodeEntry.getValue());
            }
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save cooldowns.yml: " + ex.getMessage());
        }
    }

    public void start(UUID uuid, String nodeId, long durationMillis) {
        cooldowns.computeIfAbsent(uuid, ignored -> new HashMap<>())
                .put(nodeId, System.currentTimeMillis() + durationMillis);
        save();
    }

    public long remainingMillis(UUID uuid, String nodeId) {
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null) return 0L;
        Long expiresAt = playerCooldowns.get(nodeId);
        if (expiresAt == null) return 0L;
        long remaining = expiresAt - System.currentTimeMillis();
        if (remaining <= 0L) {
            playerCooldowns.remove(nodeId);
            if (playerCooldowns.isEmpty()) cooldowns.remove(uuid);
            return 0L;
        }
        return remaining;
    }

    public boolean isCooling(UUID uuid, String nodeId) {
        return remainingMillis(uuid, nodeId) > 0L;
    }

    public Set<String> activeNodes(UUID uuid) {
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null) return Set.of();
        Set<String> active = new HashSet<>();
        for (String nodeId : new HashSet<>(playerCooldowns.keySet())) {
            if (remainingMillis(uuid, nodeId) > 0L) active.add(nodeId);
        }
        return active;
    }

    private void pruneExpired() {
        for (UUID uuid : new HashSet<>(cooldowns.keySet())) {
            activeNodes(uuid);
        }
    }
}
