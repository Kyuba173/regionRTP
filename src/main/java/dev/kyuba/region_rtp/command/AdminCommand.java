package dev.kyuba.region_rtp.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.kyuba.region_rtp.region.ConfigRtpRegionSource;
import dev.kyuba.region_rtp.region.RtpRegion;

/**
 * Admin command for managing regions and global settings.
 *
 * <p>Subcommands:
 * <ul>
 *   <li>{@code add <id> <world> <region>} — add a new region</li>
 *   <li>{@code remove <id>} — remove a region</li>
 *   <li>{@code set <id> <key> <value>} — set a per-region option</li>
 *   <li>{@code list} — list all configured regions</li>
 *   <li>{@code info <id>} — show details for one region</li>
 *   <li>{@code config <key> <value>} — set a global spawn setting</li>
 * </ul>
 *
 * <p>Changes are written to {@code config.yml} and the config is reloaded
 * immediately.
 */
public final class AdminCommand implements TabExecutor {

    private final ConfigRtpRegionSource regionSource;
    private final Runnable reloadAction;
    private final org.bukkit.plugin.java.JavaPlugin plugin;

    private static final List<String> REGION_KEYS = List.of(
            "world", "region", "require-sky-exposure", "allow-water",
            "teleport-delay-seconds", "cooldown-seconds");

    private static final List<String> GLOBAL_KEYS = List.of(
            "attempts", "edge-distance", "minimum-player-distance");

    public AdminCommand(@NotNull org.bukkit.plugin.java.JavaPlugin plugin,
                        @NotNull ConfigRtpRegionSource regionSource,
                        @NotNull Runnable reloadAction) {
        this.plugin = plugin;
        this.regionSource = regionSource;
        this.reloadAction = reloadAction;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission("regionrtp.admin")) {
            sender.sendMessage("§cYou don't have permission to use admin commands.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "set" -> handleSet(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "config" -> handleConfig(sender, args);
            case "reload" -> {
                reloadAction.run();
                sender.sendMessage("§aConfig reloaded.");
            }
            default -> sendHelp(sender, label);
        }
        return true;
    }

    private void sendHelp(@NotNull CommandSender sender, @NotNull String label) {
        sender.sendMessage("§6RegionRTP Admin Commands:");
        sender.sendMessage("§e/" + label + " add <id> <world> <region> §7— Add a new region");
        sender.sendMessage("§e/" + label + " remove <id> §7— Remove a region");
        sender.sendMessage("§e/" + label + " set <id> <key> <value> §7— Set a per-region option");
        sender.sendMessage("§e/" + label + " list §7— List all regions");
        sender.sendMessage("§e/" + label + " info <id> §7— Show details for a region");
        sender.sendMessage("§e/" + label + " config <key> <value> §7— Set a global spawn setting");
        sender.sendMessage("§e/" + label + " reload §7— Reload config from file");
        sender.sendMessage("§7Keys: §f" + String.join(", ", REGION_KEYS));
    }

    // ── add <id> <world> <region> ──────────────────────────────────────────

    private void handleAdd(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /rrtp add <id> <world> <region>");
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        String world = args[2];
        String region = args[3];

        if (regionSource.byId(id) != null) {
            sender.sendMessage("§cRegion §e" + id + "§c already exists.");
            return;
        }

        String base = "regions." + id;
        plugin.getConfig().set(base + ".world", world);
        plugin.getConfig().set(base + ".region", region);
        plugin.getConfig().set(base + ".require-sky-exposure", true);
        plugin.getConfig().set(base + ".allow-water", false);
        plugin.getConfig().set(base + ".teleport-delay-seconds", 0);
        plugin.getConfig().set(base + ".cooldown-seconds", 0);
        saveAndReload();
        sender.sendMessage("§aAdded region §e" + id + "§a (world=§e" + world
                + "§a, region=§e" + region + "§a).");
    }

    // ── remove <id> ────────────────────────────────────────────────────────

    private void handleRemove(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /rrtp remove <id>");
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);

        if (regionSource.byId(id) == null) {
            sender.sendMessage("§cNo such region: §e" + id);
            return;
        }

        plugin.getConfig().set("regions." + id, null);
        saveAndReload();
        sender.sendMessage("§aRemoved region §e" + id + "§a.");
    }

    // ── set <id> <key> <value> ──────────────────────────────────────────────

    private void handleSet(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /rrtp set <id> <key> <value>");
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        String key = args[2].toLowerCase(Locale.ROOT);
        String value = args[3];

        if (regionSource.byId(id) == null) {
            sender.sendMessage("§cNo such region: §e" + id);
            return;
        }
        if (!REGION_KEYS.contains(key)) {
            sender.sendMessage("§cUnknown key: §e" + key + "§c. Valid: §f"
                    + String.join(", ", REGION_KEYS));
            return;
        }

        String path = "regions." + id + "." + key;
        switch (key) {
            case "world", "region" -> {
                if (value.isBlank()) {
                    sender.sendMessage("§cValue cannot be blank for §e" + key);
                    return;
                }
                plugin.getConfig().set(path, value);
            }
            case "require-sky-exposure", "allow-water" -> {
                boolean bool = parseBoolean(sender, value);
                if (bool == Boolean.parseBoolean("error")) return;
                plugin.getConfig().set(path, bool);
            }
            case "teleport-delay-seconds", "cooldown-seconds" -> {
                int num = parseInt(sender, value, key);
                if (num == Integer.MIN_VALUE) return;
                plugin.getConfig().set(path, num);
            }
            default -> {
                sender.sendMessage("§cUnknown key: §e" + key);
                return;
            }
        }
        saveAndReload();
        sender.sendMessage("§aSet §e" + key + "§a=§e" + value + "§a for region §e" + id + "§a.");
    }

    // ── list ───────────────────────────────────────────────────────────────

    private void handleList(@NotNull CommandSender sender) {
        var regions = regionSource.regions();
        if (regions.isEmpty()) {
            sender.sendMessage("§7No regions configured.");
            return;
        }
        sender.sendMessage("§6Configured regions (" + regions.size() + "):");
        for (RtpRegion r : regions) {
            sender.sendMessage("§e" + r.id() + " §7→ §f" + r.worldName()
                    + ":" + r.regionId());
        }
    }

    // ── info <id> ──────────────────────────────────────────────────────────

    private void handleInfo(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /rrtp info <id>");
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        RtpRegion r = regionSource.byId(id);
        if (r == null) {
            sender.sendMessage("§cNo such region: §e" + id);
            return;
        }
        sender.sendMessage("§6Region: §e" + r.id());
        sender.sendMessage("§7  world: §f" + r.worldName());
        sender.sendMessage("§7  region: §f" + r.regionId());
        sender.sendMessage("§7  require-sky-exposure: §f" + r.requireSkyExposure());
        sender.sendMessage("§7  allow-water: §f" + r.allowWater());
        sender.sendMessage("§7  teleport-delay-seconds: §f" + r.teleportDelaySeconds());
        sender.sendMessage("§7  cooldown-seconds: §f" + r.cooldownSeconds());
    }

    // ── config <key> <value> ────────────────────────────────────────────────

    private void handleConfig(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /rrtp config <key> <value>");
            sender.sendMessage("§7Keys: §f" + String.join(", ", GLOBAL_KEYS));
            return;
        }
        String key = args[1].toLowerCase(Locale.ROOT);
        String value = args[2];

        if (!GLOBAL_KEYS.contains(key)) {
            sender.sendMessage("§cUnknown key: §e" + key + "§c. Valid: §f"
                    + String.join(", ", GLOBAL_KEYS));
            return;
        }

        String path = "spawn." + key;
        switch (key) {
            case "attempts" -> {
                int num = parseInt(sender, value, key);
                if (num == Integer.MIN_VALUE) return;
                plugin.getConfig().set(path, num);
            }
            case "edge-distance", "minimum-player-distance" -> {
                double num = parseDouble(sender, value, key);
                if (num == Double.MIN_VALUE) return;
                plugin.getConfig().set(path, num);
            }
            default -> {
                sender.sendMessage("§cUnknown key: §e" + key);
                return;
            }
        }
        saveAndReload();
        sender.sendMessage("§aSet §e" + key + "§a=§e" + value + "§a.");
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private void saveAndReload() {
        plugin.saveConfig();
        reloadAction.run();
    }

    private boolean parseBoolean(@NotNull CommandSender sender, @NotNull String value) {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes")) {
            return true;
        }
        if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no")) {
            return false;
        }
        sender.sendMessage("§cInvalid boolean: §e" + value + "§c (true/false)");
        return Boolean.parseBoolean("error");
    }

    private int parseInt(@NotNull CommandSender sender, @NotNull String value, @NotNull String key) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number: §e" + value);
            return Integer.MIN_VALUE;
        }
    }

    private double parseDouble(@NotNull CommandSender sender, @NotNull String value, @NotNull String key) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number: §e" + value);
            return Double.MIN_VALUE;
        }
    }

    // ── tab completion ─────────────────────────────────────────────────────

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission("regionrtp.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return filter(List.of("add", "remove", "set", "list", "info", "config", "reload"), args[0]);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "remove", "info" -> {
                if (args.length == 2) {
                    return filter(regionIds(), args[1]);
                }
            }
            case "set" -> {
                if (args.length == 2) {
                    return filter(regionIds(), args[1]);
                }
                if (args.length == 3) {
                    return filter(REGION_KEYS, args[2]);
                }
                if (args.length == 4) {
                    String key = args[2].toLowerCase(Locale.ROOT);
                    if (key.equals("require-sky-exposure") || key.equals("allow-water")) {
                        return filter(List.of("true", "false"), args[3]);
                    }
                    if (key.equals("world") && sender instanceof org.bukkit.entity.Player p) {
                        return filter(plugin.getServer().getWorlds().stream()
                                .map(w -> w.getName()).toList(), args[3]);
                    }
                }
            }
            case "config" -> {
                if (args.length == 2) {
                    return filter(GLOBAL_KEYS, args[1]);
                }
            }
            case "add" -> {
                if (args.length == 3 && sender instanceof org.bukkit.entity.Player) {
                    return filter(plugin.getServer().getWorlds().stream()
                            .map(w -> w.getName()).toList(), args[2]);
                }
            }
            default -> {}
        }
        return List.of();
    }

    private List<String> regionIds() {
        return regionSource.regions().stream().map(RtpRegion::id).toList();
    }

    private List<String> filter(@NotNull List<String> options, @NotNull String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(p)) {
                result.add(o);
            }
        }
        return result;
    }
}