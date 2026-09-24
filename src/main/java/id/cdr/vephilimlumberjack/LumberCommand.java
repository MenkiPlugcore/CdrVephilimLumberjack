package id.cdr.vephilimlumberjack;

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
            sender.sendMessage(color("&e/lumber node <create|remove|list|info>"));
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "create" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(color("&cCommand ini harus dijalankan oleh player."));
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage(color("&e/lumber node create <id>"));
                    return;
                }
                Block target = player.getTargetBlockExact(6);
                if (target == null || !Tag.LOGS.isTagged(target.getType())) {
                    sender.sendMessage(color("&cArahkan crosshair ke blok log pohon dalam jarak 6 blok."));
                    return;
                }
                if (plugin.nodes().get(args[2]) != null) {
                    sender.sendMessage(color("&cNode dengan ID itu sudah ada."));
                    return;
                }
                TreeNode node = plugin.nodes().create(args[2], target);
                sender.sendMessage(color("&aNode &f" + node.id() + " &aberhasil dibuat di &f"
                        + node.worldName() + " " + node.x() + " " + node.y() + " " + node.z() + "&a."));
            }
            case "remove" -> {
                if (args.length < 3) {
                    sender.sendMessage(color("&e/lumber node remove <id>"));
                    return;
                }
                if (plugin.nodes().remove(args[2])) sender.sendMessage(color("&aNode berhasil dihapus."));
                else sender.sendMessage(color("&cNode tidak ditemukan."));
            }
            case "list" -> {
                Collection<TreeNode> nodes = plugin.nodes().all();
                if (nodes.isEmpty()) {
                    sender.sendMessage(color("&7Belum ada lumber node."));
                    return;
                }
                sender.sendMessage(color("&6Lumber Nodes &7(" + nodes.size() + "): &f"
                        + nodes.stream().map(TreeNode::id).collect(Collectors.joining(", "))));
            }
            case "info" -> {
                if (args.length < 3) {
                    sender.sendMessage(color("&e/lumber node info <id>"));
                    return;
                }
                TreeNode node = plugin.nodes().get(args[2]);
                if (node == null) {
                    sender.sendMessage(color("&cNode tidak ditemukan."));
                    return;
                }
                sender.sendMessage(color("&6Node: &f" + node.id()));
                sender.sendMessage(color("&7Lokasi: &f" + node.worldName() + " " + node.x() + " " + node.y() + " " + node.z()));
                sender.sendMessage(color("&7Block: &f" + (node.block() == null ? "WORLD_UNLOADED" : node.block().getType().name())));
            }
            default -> sender.sendMessage(color("&e/lumber node <create|remove|list|info>"));
        }
    }

    private void handleTimber(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cdrvephilimlumberjack.timber")) {
            noPermission(sender);
            return;
        }
        if (args.length < 3 || !args[1].equalsIgnoreCase("give")) {
            sender.sendMessage(color("&e/lumber timber give <player> [amount]"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(color("&cPlayer tidak online."));
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Math.min(2304, Integer.parseInt(args[3])));
            } catch (NumberFormatException ex) {
                sender.sendMessage(color("&cAmount tidak valid."));
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
        sender.sendMessage(color("&aMemberikan &f" + amount + " Vephilim Timber &ake &f" + target.getName() + "&a."));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("cdrvephilimlumberjack.reload")) {
            noPermission(sender);
            return;
        }
        plugin.reloadPlugin();
        sender.sendMessage(color("&aCdrVephilimLumberjack berhasil direload."));
    }

    private void handleStatus(CommandSender sender) {
        if (!sender.hasPermission("cdrvephilimlumberjack.status")) {
            noPermission(sender);
            return;
        }
        sender.sendMessage(color("&6CdrVephilimLumberjack &fv" + plugin.getPluginMeta().getVersion()));
        sender.sendMessage(color("&7Nodes: &f" + plugin.nodes().size()));
        sender.sendMessage(color("&7Hits per harvest: &f" + plugin.getConfig().getInt("settings.hits-required", 3)));
        sender.sendMessage(color("&7Cooldown: &f" + plugin.getConfig().getLong("settings.node-cooldown-seconds", 30L) + "s"));
        sender.sendMessage(color("&7Required tool: &f" + plugin.requiredTool().name()));
        sender.sendMessage(color("&7Economy integration: &f" + (Bukkit.getPluginManager().isPluginEnabled("CdrVephilimEconomy") ? "DETECTED" : "NOT DETECTED")));
    }

    private void help(CommandSender sender) {
        sender.sendMessage(color("&6CdrVephilimLumberjack"));
        sender.sendMessage(color("&e/lumber node create <id>"));
        sender.sendMessage(color("&e/lumber node remove <id>"));
        sender.sendMessage(color("&e/lumber node list"));
        sender.sendMessage(color("&e/lumber node info <id>"));
        sender.sendMessage(color("&e/lumber timber give <player> [amount]"));
        sender.sendMessage(color("&e/lumber reload"));
        sender.sendMessage(color("&e/lumber status"));
    }

    private void noPermission(CommandSender sender) {
        sender.sendMessage(color("&cKamu tidak memiliki permission untuk command ini."));
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
