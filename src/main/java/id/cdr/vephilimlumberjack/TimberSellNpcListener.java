package id.cdr.vephilimlumberjack;

import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TimberSellNpcListener implements Listener {
    private final CdrVephilimLumberjack plugin;
    private final Economy economy;
    private final Map<UUID, Long> lastInteraction = new HashMap<>();

    public TimberSellNpcListener(CdrVephilimLumberjack plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onNpcRightClick(NPCRightClickEvent event) {
        if (!plugin.getConfig().getBoolean("npc-sell.enabled", true)) return;

        double pricePerTimber = priceForNpc(event.getNPC().getId());
        if (pricePerTimber == Double.NEGATIVE_INFINITY) return;

        Player player = event.getClicker();
        event.setCancelled(true);

        long now = System.currentTimeMillis();
        long delay = Math.max(0L, plugin.getConfig().getLong("npc-sell.interaction-cooldown-ms", 750L));
        long previous = lastInteraction.getOrDefault(player.getUniqueId(), 0L);
        if (now - previous < delay) return;
        lastInteraction.put(player.getUniqueId(), now);

        if (!Double.isFinite(pricePerTimber) || pricePerTimber <= 0.0D) {
            action(player, plugin.message("messages.npc-invalid-price",
                    "&cNPC pembeli Timber ini belum memiliki harga yang valid."));
            return;
        }

        int timberAmount = countTimber(player);
        if (timberAmount <= 0) {
            action(player, plugin.message("messages.npc-no-timber",
                    "&eKamu tidak memiliki Vephilim Timber untuk dijual."));
            return;
        }

        int removedAmount = removeAllTimber(player);
        if (removedAmount <= 0) {
            action(player, plugin.message("messages.npc-no-timber",
                    "&eKamu tidak memiliki Vephilim Timber untuk dijual."));
            return;
        }

        double total = pricePerTimber * removedAmount;
        EconomyResponse response = economy.depositPlayer(player, total);
        if (!response.transactionSuccess()) {
            returnTimber(player, removedAmount);
            plugin.getLogger().warning("Failed to pay Timber sale for " + player.getName()
                    + ": " + response.errorMessage);
            action(player, plugin.message("messages.npc-sale-failed",
                    "&cPenjualan gagal. Timber kamu sudah dikembalikan."));
            return;
        }

        action(player, plugin.message("messages.npc-sold",
                        "&aTerjual &f{amount} Timber &7| &a+{money}")
                .replace("{amount}", String.valueOf(removedAmount))
                .replace("{money}", economy.format(total)));
    }

    private double priceForNpc(int npcId) {
        ConfigurationSection npcs = plugin.getConfig().getConfigurationSection("npc-sell.npcs");
        if (npcs == null) return Double.NEGATIVE_INFINITY;

        String key = String.valueOf(npcId);
        if (!npcs.contains(key)) return Double.NEGATIVE_INFINITY;

        Object raw = npcs.get(key);
        if (raw instanceof Number number) {
            return number.doubleValue();
        }

        ConfigurationSection section = npcs.getConfigurationSection(key);
        if (section == null) return Double.NaN;
        if (!section.getBoolean("enabled", true)) return Double.NEGATIVE_INFINITY;
        return section.getDouble("price-per-timber", Double.NaN);
    }

    private int countTimber(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (plugin.timber().isTimber(item)) total += item.getAmount();
        }
        return total;
    }

    private int removeAllTimber(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] storage = inventory.getStorageContents();
        int total = 0;

        for (int i = 0; i < storage.length; i++) {
            ItemStack item = storage[i];
            if (!plugin.timber().isTimber(item)) continue;
            total += item.getAmount();
            storage[i] = null;
        }

        inventory.setStorageContents(storage);
        return total;
    }

    private void returnTimber(Player player, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            int stackAmount = Math.min(64, remaining);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(plugin.timber().create(stackAmount));
            for (ItemStack item : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
            remaining -= stackAmount;
        }
    }

    private void action(Player player, String legacyColoredText) {
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(legacyColoredText));
    }
}
