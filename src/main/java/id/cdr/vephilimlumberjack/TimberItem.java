package id.cdr.vephilimlumberjack;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class TimberItem {
    private final CdrVephilimLumberjack plugin;
    private final NamespacedKey resourceKey;
    private final NamespacedKey itemIdKey;

    public TimberItem(CdrVephilimLumberjack plugin) {
        this.plugin = plugin;
        this.resourceKey = new NamespacedKey(plugin, "vephilim_resource");
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
    }

    public ItemStack create(int amount) {
        FileConfiguration config = plugin.getConfig();
        Material material = Material.matchMaterial(config.getString("resource.material", "OAK_LOG"));
        if (material == null || material.isAir()) material = Material.OAK_LOG;

        ItemStack item = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(config.getString("resource.name", "&6Vephilim Timber")));

        List<String> lore = new ArrayList<>();
        for (String line : config.getStringList("resource.lore")) lore.add(color(line));
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(resourceKey, PersistentDataType.STRING, "lumber");
        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, "vephilim_timber");
        item.setItemMeta(meta);
        return item;
    }

    public boolean isTimber(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String id = item.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        return "vephilim_timber".equals(id);
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
