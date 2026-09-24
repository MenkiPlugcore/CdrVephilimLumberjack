package id.cdr.vephilimlumberjack;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class LumberCommand implements CommandExecutor, TabCompleter {
    private final CdrVephilimLumberjack plugin;

    public LumberCommand(CdrVephilimLumberjack plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "node" -> handleNode(sender, args);
            case "timber" -> handleTimber(sender, args);
            case "reload" -> handleReload(sender);
            case "status" -> handleStatus(sender);
            default -> help(sender);
        }
        return true;
    }

    private void handleNode(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cdrvephilimlumberjack.node")) {
            noPermission(sender);
            return;
        }
        if (args.length < 2) {
            ui(sender, "&e/lumber node <create|remove|list|info>");
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "create" -> {
                if (!(sender instanceof Player player)) {
                    ui(sender, "&cCommand ini harus dijalankan oleh player.");
                    return;
                }
                if (args.length < 3) {
                    ui(sender, "&e/lumber node create <id>");
                    return;
                }
                Block target = player.getTargetBlockExact(6);
                if (target == null || !Tag.LOGS.isTagged(target.getType())) {
                    ui(sender, "&cArahkan crosshair ke blok log pohon dalam jarak 6 blok.");
                    return;
                }
                if (plugin.nodes().get(args[2]) != null) {
                    ui(sender, "&cNode dengan ID itu sudah ada.");
                    return;
                }
                TreeNode node = plugin.nodes().create(args[2], target);
                ui(sender, "&aNode &f" + node.id() + " &aberhasil dibuat di &f"
                        + node.worldName() + " " + node.x() + " " + node.y() + " " + node.z() + "&a.");
            }
            case "remove" -> {
                if (args.length < 3) {
                    ui(sender, "&e/lumber node remove <id>");
                    return;
                }
                if (plugin.nodes().remove(args[2])) ui(sender, "&aNode berhasil dihapus.");
                else ui(sender, "&cNode tidak ditemukan.");
            }
            case "list" -> {
                Collection<TreeNode> nodes = plugin.nodes().all();
                if (nodes.isEmpty()) {
                    ui(sender, "&7Belum ada lumber node.");
                    return;
                }
                ui(sender, "&6Lumber Nodes &7(" + nodes.size() + "): &f"
                        + nodes.stream().map(TreeNode::id).collect(Collectors.joining(", ")));
            }
            case "info" -> {
                if (args.length < 3) {
                    ui(sender, "&e/lumber node info <id>");
                    return;
                }
                TreeNode node = plugin.nodes().get(args[2]);
                if (node == null) {
                    ui(sender, "&cNode tidak ditemukan.");
                    return;
                }
                ui(sender, "&6" + node.id() + " &7| &f" + node.worldName() + " " + node.x() + " " + node.y() + " " + node.z()
                        + " &7| &f" + (node.block() == null ? "WORLD_UNLOADED" : node.block().getType().name()));
            }
            default -> ui(sender, "&e/lumber node <create|remove|list|info>");
        }
    }

    private void handleTimber(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cdrvephilimlumberjack.timber")) {
            noPermission(sender);
            return;
        }
        if (args.length < 3 || !args[1].equalsIgnoreCase("give")) {
            ui(sender, "&e/lumber timber give <player> [amount]");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            ui(sender, "&cPlayer tidak online.");
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Math.min(2304, Integer.parseInt(args[3])));
            } catch (NumberFormatException ex) {
                ui(sender, "&cAmount tidak valid.");
                return;
            }
        }

        int remaining = amount;
        while (remaining > 0) {
            int stackAmount = Math.min(64, remaining);
            ItemStack stack = plugin.timber().create(stackAmount);
            target.getInventory().addItem(stack).values()
                    .forEach(leftover -> target.getWorld().dropItemNaturally(target.getLocation(), leftover));
            remaining -= stackAmount;
        }
        ui(sender, "&aMemberikan &f" + amount + " Vephilim Timber &ake &f" + target.getName() + "&a.");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("cdrvephilimlumberjack.reload")) {
            noPermission(sender);
            return;
        }
        plugin.reloadPlugin();
        ui(sender, "&aCdrVephilimLumberjack berhasil direload.");
    }

    private void handleStatus(CommandSender sender) {
        if (!sender.hasPermission("cdrvephilimlumberjack.status")) {
            noPermission(sender);
            return;
        }
        String economy = Bukkit.getPluginManager().isPluginEnabled("CdrVephilimEconomy") ? "DETECTED" : "NOT DETECTED";
        ui(sender, "&6Lumberjack &fv" + plugin.getPluginMeta().getVersion()
                + " &7| Nodes: &f" + plugin.nodes().size()
                + " &7| Hits: &f" + plugin.getConfig().getInt("settings.hits-required", 3)
                + " &7| Cooldown: &f" + plugin.getConfig().getLong("settings.node-cooldown-seconds", 30L) + "s"
                + " &7| Economy: &f" + economy);
    }

    private void help(CommandSender sender) {
        ui(sender, "&6/lumber &7| &fnode create/remove/list/info &7| &ftimber give &7| &freload &7| &fstatus");
    }

    private void noPermission(CommandSender sender) {
        ui(sender, "&cKamu tidak memiliki permission untuk command ini.");
    }

    private void ui(CommandSender sender, String input) {
        String colored = color(input);
        if (sender instanceof Player player) {
            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(colored));
            return;
        }
        sender.sendMessage(colored);
    }

    private String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(List.of("node", "timber", "reload", "status"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("node")) {
            return filter(List.of("create", "remove", "list", "info"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("timber")) return filter(List.of("give"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("node")
                && (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("info"))) {
            return filter(plugin.nodes().all().stream().map(TreeNode::id).toList(), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("timber") && args[1].equalsIgnoreCase("give")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String input) {
        String needle = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(needle)) result.add(value);
        }
        return result;
    }
}
