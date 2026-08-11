package dev.kyuba.region_rtp.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.kyuba.region_rtp.region.RtpRegion;
import dev.kyuba.region_rtp.region.RtpRegionSource;
import dev.kyuba.region_rtp.spawn.TeleportService;

/**
 * Implements {@code /regionrtp [region]} and {@code /regionrtp reload}.
 */
public final class RegionRtpCommand implements TabExecutor {

    private final RtpRegionSource regionSource;
    private final TeleportService teleportService;
    private final Runnable reloadAction;

    public RegionRtpCommand(@NotNull RtpRegionSource regionSource,
                            @NotNull TeleportService teleportService,
                            @NotNull Runnable reloadAction) {
        this.regionSource = regionSource;
        this.teleportService = teleportService;
        this.reloadAction = reloadAction;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /" + label + " <region> | reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("regionrtp.admin.reload")) {
                sender.sendMessage("§cYou don't have permission to reload the config.");
                return true;
            }
            reloadAction.run();
            sender.sendMessage("§aRegionRTP config reloaded.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly a player can use this command.");
            return true;
        }

        String regionId = args[0].toLowerCase(Locale.ROOT);
        RtpRegion region = regionSource.byId(regionId);
        if (region == null) {
            sender.sendMessage("§cUnknown region: §e" + regionId);
            return true;
        }

        // Per-region permission: regionrtp.<regionId> or regionrtp.*
        String regionPermission = "regionrtp." + regionId;
        if (!sender.hasPermission(regionPermission) && !sender.hasPermission("regionrtp.*")) {
            sender.sendMessage("§cYou don't have permission to teleport to §e" + regionId + "§c.");
            return true;
        }

        int delay = region.teleportDelaySeconds();
        if (delay > 0) {
            sender.sendMessage("§7Searching for a safe spawn in §e" + regionId
                    + "§7… §c(stand still for §e" + delay + "§c seconds after a spot is found)");
        } else {
            sender.sendMessage("§7Searching for a safe spawn in §e" + regionId + "§7…");
        }
        TeleportService.Outcome outcome = teleportService.teleport(player, region);
        switch (outcome) {
            case SUCCESS -> {
                if (delay == 0) {
                    sender.sendMessage("§aTeleported to §e" + regionId + "§a.");
                } else {
                    sender.sendMessage("§eSafe spot found! Stand still for §e" + delay
                            + "§e seconds to complete the teleport.");
                }
            }
            case WORLD_NOT_FOUND -> sender.sendMessage(
                    "§cWorld '§e" + region.worldName() + "§c' is not loaded.");
            case WORLDGUARD_UNAVAILABLE -> sender.sendMessage(
                    "§cWorldGuard region manager is unavailable for world '§e"
                            + region.worldName() + "§c'.");
            case REGION_NOT_FOUND -> sender.sendMessage(
                    "§cWorldGuard region '§e" + region.regionId()
                            + "§c' not found in world '§e" + region.worldName() + "§c'.");
            case NO_SAFE_LOCATION -> sender.sendMessage(
                    "§cNo safe spawn location found in §e" + regionId
                            + "§c after all attempts.");
            case ON_COOLDOWN -> {} // message already sent by TeleportService
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        if ("reload".startsWith(prefix) && sender.hasPermission("regionrtp.admin.reload")) {
            result.add("reload");
        }
        boolean wildcard = sender.hasPermission("regionrtp.*");
        for (RtpRegion r : regionSource.regions()) {
            if (r.id().startsWith(prefix) && (wildcard || sender.hasPermission("regionrtp." + r.id()))) {
                result.add(r.id());
            }
        }
        return result;
    }
}