package id.cdr.vephilimlumberjack;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class LumberListener implements Listener {
    private final CdrVephilimLumberjack plugin;
    private final Map<UUID, HitState> states = new HashMap<>();

    public LumberListener(CdrVephilimLumberjack plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        Action interactAction = event.getAction();
        if (interactAction != Action.LEFT_CLICK_BLOCK && interactAction != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        TreeNode node = plugin.nodes().getByBlock(block);
        if (node == null) return;

        // Registered tree interactions are owned by the lumber system. This also
        // prevents right-clicking with an axe from stripping the real server log.
        event.setCancelled(true);

        Player player = event.getPlayer();

        if (!Tag.LOGS.isTagged(block.getType())) {
            action(player, plugin.message("messages.hit-leaves", "&eTebang bagian batang pohon."));
            return;
        }

        double maxDistance = Math.max(0.0D, plugin.getConfig().getDouble("chop-range.max-distance", 3.5D));
        if (plugin.getConfig().getBoolean("chop-range.enabled", true)
                && maxDistance > 0.0D
                && !isNearTree(player, node, maxDistance)) {
            action(player, plugin.message("messages.too-far", "&cTerlalu jauh dari pohon. &7Maks. &f{distance} blok")
                    .replace("{distance}", formatDistance(maxDistance)));
            return;
        }

        long remaining = plugin.cooldowns().remainingMillis(player.getUniqueId(), node.id());
        if (remaining > 0L) {
            long seconds = Math.max(1L, (remaining + 999L) / 1000L);
            action(player, plugin.message("messages.cooldown", "&7Pohon ini sedang tumbuh kembali. &f{seconds}s")
                    .replace("{seconds}", String.valueOf(seconds)));
            return;
        }

        // Safety net: if an older scheduled restore was missed, make the original
        // tree visible again as soon as the node is actually available.
        plugin.visuals().restoreIfReady(player, node);

        Material requiredTool = plugin.requiredTool();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() != requiredTool) {
            action(player, plugin.message("messages.wrong-tool", "&cGunakan Diamond Axe untuk menebang pohon ini."));
            return;
        }

        long now = System.currentTimeMillis();
        long hitDelay = Math.max(0L, plugin.getConfig().getLong("settings.hit-delay-ms", 650L));
        long resetAfter = Math.max(hitDelay, plugin.getConfig().getLong("settings.progress-reset-ms", 5000L));
        HitState state = states.get(player.getUniqueId());

        if (state == null || !state.nodeId.equals(node.id()) || now - state.lastActivity > resetAfter) {
            state = new HitState(node.id(), 0, 0L, now);
            states.put(player.getUniqueId(), state);
        }
        if (now - state.lastHit < hitDelay) return;

        state.lastHit = now;
        state.lastActivity = now;
        state.hits++;

        damageTool(player, hand, Math.max(0, plugin.getConfig().getInt("settings.durability-per-hit", 1)));
        player.playSound(block.getLocation(), Sound.BLOCK_WOOD_HIT, 0.9f, 0.95f + (state.hits * 0.05f));

        int requiredHits = Math.max(1, plugin.getConfig().getInt("settings.hits-required", 3));
        if (state.hits < requiredHits) {
            String bar = progressBar(state.hits, requiredHits);
            action(player, plugin.message("messages.progress", "&6Menebang... &f{bar} &7({hit}/{max})")
                    .replace("{bar}", bar)
                    .replace("{hit}", String.valueOf(state.hits))
                    .replace("{max}", String.valueOf(requiredHits)));
            return;
        }

        states.remove(player.getUniqueId());
        int rewardAmount = Math.max(1, plugin.getConfig().getInt("settings.reward-amount", 1));
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(plugin.timber().create(rewardAmount));
        for (ItemStack item : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }

        long cooldownMillis = Math.max(1L, plugin.getConfig().getLong("settings.node-cooldown-seconds", 30L)) * 1000L;
        plugin.cooldowns().start(player.getUniqueId(), node.id(), cooldownMillis);
        plugin.visuals().hideFor(player, node);
        plugin.visuals().scheduleRestore(player, node, cooldownMillis);

        player.playSound(block.getLocation(), Sound.BLOCK_WOOD_BREAK, 1.0f, 0.9f);
        action(player, plugin.message("messages.harvested", "&a+{amount} Vephilim Timber")
                .replace("{amount}", String.valueOf(rewardAmount)));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent event) {
        if (plugin.nodes().getByBlock(event.getBlock()) != null) event.setCancelled(true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.visuals().reapplyActiveCooldowns(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        states.remove(event.getPlayer().getUniqueId());
    }

    private boolean isNearTree(Player player, TreeNode node, double maxDistance) {
        Location location = player.getLocation();
        double maxDistanceSquared = maxDistance * maxDistance;

        for (Block treeBlock : plugin.nodes().blocks(node)) {
            if (!Tag.LOGS.isTagged(treeBlock.getType())) continue;
            if (!treeBlock.getWorld().equals(location.getWorld())) continue;
            if (distanceSquaredToBlock(location, treeBlock) <= maxDistanceSquared) return true;
        }

        Block root = node.block();
        return root != null
                && root.getWorld().equals(location.getWorld())
                && distanceSquaredToBlock(location, root) <= maxDistanceSquared;
    }

    private double distanceSquaredToBlock(Location location, Block block) {
        double dx = axisDistance(location.getX(), block.getX(), block.getX() + 1.0D);
        double dy = axisDistance(location.getY(), block.getY(), block.getY() + 1.0D);
        double dz = axisDistance(location.getZ(), block.getZ(), block.getZ() + 1.0D);
        return (dx * dx) + (dy * dy) + (dz * dz);
    }

    private double axisDistance(double value, double min, double max) {
        if (value < min) return min - value;
        if (value > max) return value - max;
        return 0.0D;
    }

    private String formatDistance(double distance) {
        return String.format(Locale.US, "%.1f", distance);
    }

    private void damageTool(Player player, ItemStack item, int amount) {
        if (amount <= 0 || item.getType().getMaxDurability() <= 0 || !(item.getItemMeta() instanceof Damageable damageable)) return;
        int newDamage = damageable.getDamage() + amount;
        if (newDamage >= item.getType().getMaxDurability()) {
            item.setAmount(0);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            return;
        }
        damageable.setDamage(newDamage);
        item.setItemMeta(damageable);
    }

    private String progressBar(int hit, int max) {
        StringBuilder bar = new StringBuilder("&f");
        for (int i = 1; i <= max; i++) bar.append(i <= hit ? "■" : "□");
        return plugin.color(bar.toString());
    }

    private void action(Player player, String legacyColoredText) {
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(legacyColoredText));
    }

    private static final class HitState {
        private final String nodeId;
        private int hits;
        private long lastHit;
        private long lastActivity;

        private HitState(String nodeId, int hits, long lastHit, long lastActivity) {
            this.nodeId = nodeId;
            this.hits = hits;
            this.lastHit = lastHit;
            this.lastActivity = lastActivity;
        }
    }
}
