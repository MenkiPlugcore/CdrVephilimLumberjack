package id.cdr.vephilimlumberjack;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class CdrVephilimLumberjack extends JavaPlugin {
    private NodeManager nodeManager;
    private CooldownManager cooldownManager;
    private TimberItem timberItem;
    private VisualManager visualManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        nodeManager = new NodeManager(this);
        cooldownManager = new CooldownManager(this);
        timberItem = new TimberItem(this);
        visualManager = new VisualManager(this);

        nodeManager.load();
        cooldownManager.load();

        LumberCommand lumberCommand = new LumberCommand(this);
        PluginCommand command = getCommand("lumber");
        if (command == null) {
            getLogger().severe("Command /lumber is missing from plugin.yml. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(lumberCommand);
        command.setTabCompleter(lumberCommand);

        getServer().getPluginManager().registerEvents(new LumberListener(this), this);
        getLogger().info("CdrVephilimLumberjack v" + getPluginMeta().getVersion() + " enabled with " + nodeManager.size() + " node(s).");
    }

    @Override
    public void onDisable() {
        if (nodeManager != null) nodeManager.save();
        if (cooldownManager != null) cooldownManager.save();
    }

    public void reloadPlugin() {
        reloadConfig();
        nodeManager.load();
        cooldownManager.load();
    }

    public NodeManager nodes() {
        return nodeManager;
    }

    public CooldownManager cooldowns() {
        return cooldownManager;
    }

    public TimberItem timber() {
        return timberItem;
    }

    public VisualManager visuals() {
        return visualManager;
    }

    public Material requiredTool() {
        Material material = Material.matchMaterial(getConfig().getString("settings.required-tool", "DIAMOND_AXE"));
        return material == null ? Material.DIAMOND_AXE : material;
    }

    public String message(String path, String fallback) {
        return color(getConfig().getString(path, fallback));
    }

    public String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}
