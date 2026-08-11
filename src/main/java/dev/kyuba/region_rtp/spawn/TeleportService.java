package dev.kyuba.region_rtp.spawn;

import java.util.random.RandomGenerator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.kyuba.region_rtp.config.SpawnConfig;
import dev.kyuba.region_rtp.region.RtpRegion;
import dev.kyuba.region_rtp.worldguard.ResolvedRegion;
import dev.kyuba.region_rtp.worldguard.WorldGuardResolver;

/**
 * Finds a safe teleport destination inside a configured WorldGuard region
 * and teleports a player there.
 *
 * <p>The search is bounded by {@link SpawnConfig#attempts()} and always
 * performs a final WorldGuard containment check. If the region has a
 * non-zero {@code teleportDelaySeconds}, the teleport is applied via a
 * {@link TeleportTask} that requires the player to stand still for the
 * configured duration.
 */
public final class TeleportService {

    public enum Outcome {
        SUCCESS,
        WORLD_NOT_FOUND,
        WORLDGUARD_UNAVAILABLE,
        REGION_NOT_FOUND,
        NO_SAFE_LOCATION,
        ON_COOLDOWN
    }

    private final Plugin plugin;
    private final WorldGuardResolver resolver;
    private final CandidateSelector selector;
    private final SafeLocationValidator validator;
    private final java.util.Map<java.util.UUID, Long> cooldownExpiry = new java.util.concurrent.ConcurrentHashMap<>();

    public TeleportService(@NotNull Plugin plugin, @NotNull WorldGuardResolver resolver,
                           @NotNull SpawnConfig config, @NotNull RandomGenerator random) {
        this.plugin = plugin;
        this.resolver = resolver;
        this.selector = new CandidateSelector(config, random);
        this.validator = new SafeLocationValidator();
    }

    /**
     * Attempt to find and apply a randomized safe teleport for the given player
     * into the given region.
     *
     * <p>If the region has a non-zero teleport delay, the teleport is scheduled
     * and the player must stand still until it completes. In that case the
     * returned outcome is {@link Outcome#SUCCESS} once the search succeeds and
     * the task is started — the actual teleport happens after the delay.
     *
     * @return the outcome (never throws)
     */
    public @NotNull Outcome teleport(@NotNull Player player, @NotNull RtpRegion region) {
        // Cooldown check
        int cooldownSeconds = region.cooldownSeconds();
        if (cooldownSeconds > 0) {
            Long expiry = cooldownExpiry.get(player.getUniqueId());
            long now = System.currentTimeMillis();
            if (expiry != null && expiry > now) {
                long remaining = (expiry - now) / 1000;
                player.sendMessage("§cTeleport on cooldown: §e" + Math.max(1, remaining)
                        + "§c seconds remaining.");
                return Outcome.ON_COOLDOWN;
            }
        }

        ResolvedRegion resolved = resolver.resolve(region.worldName(), region.regionId());
        if (resolved == null) {
            // Distinguish world-not-found from region-not-found for clearer messages
            World world = resolver.world(region.worldName());
            if (world == null) {
                return Outcome.WORLD_NOT_FOUND;
            }
            if (resolver.regionManager(world) == null) {
                return Outcome.WORLDGUARD_UNAVAILABLE;
            }
            return Outcome.REGION_NOT_FOUND;
        }

        SafeLocationValidator.Options options = new SafeLocationValidator.Options(
                region.requireSkyExposure(), region.allowWater());

        Location destination = findSafeLocation(resolved, player, options);
        if (destination == null) {
            return Outcome.NO_SAFE_LOCATION;
        }

        // Always face toward the horizontal centre of the region after teleporting
        faceTowardRegionCenter(destination, resolved);

        int delay = region.teleportDelaySeconds();
        if (delay > 0) {
            new TeleportTask(plugin, player, destination, delay,
                    () -> {
                        // Set cooldown after the delayed teleport actually completes
                        setCooldown(player, region);
                        player.sendMessage("§aTeleporting…");
                    },
                    () -> {})
                    .start();
        } else {
            player.teleport(destination);
            setCooldown(player, region);
        }
        return Outcome.SUCCESS;
    }

    private void setCooldown(@NotNull Player player, @NotNull RtpRegion region) {
        int cooldownSeconds = region.cooldownSeconds();
        if (cooldownSeconds > 0) {
            cooldownExpiry.put(player.getUniqueId(),
                    System.currentTimeMillis() + (cooldownSeconds * 1000L));
        }
    }

    /**
     * Find a safe location, preferring player-distance separation, with a
     * bounded fallback that relaxes the separation preference.
     */
    public @Nullable Location findSafeLocation(@NotNull ResolvedRegion resolved, @NotNull Player player,
                                               @NotNull SafeLocationValidator.Options options) {
        World world = resolved.world();
        RegionBounds bounds = boundsFor(resolved, world);

        // Phase 1: prefer candidates satisfying player distance
        Location found = search(resolved, world, bounds, player, selector.maxAttempts(), true, options);
        if (found != null) {
            return found;
        }

        // Phase 2: bounded fallback ignoring player distance
        int fallback = selector.fallbackAttempts();
        return search(resolved, world, bounds, player, fallback, false, options);
    }

    private @Nullable Location search(@NotNull ResolvedRegion resolved,
                                      @NotNull World world,
                                      @NotNull RegionBounds bounds,
                                      @NotNull Player player,
                                      int attempts,
                                      boolean enforcePlayerDistance,
                                      @NotNull SafeLocationValidator.Options options) {
        double minPlayerDist = selector.minimumPlayerDistance();
        for (int i = 0; i < attempts; i++) {
            int x = selector.sampleX(bounds);
            int z = selector.sampleZ(bounds);

            if (!selector.isEdgeNear(x, z, bounds)) {
                continue;
            }

            int y = findSuitableY(resolved, world, x, z, bounds, options);
            if (y == Integer.MIN_VALUE) {
                continue;
            }

            Block feet = world.getBlockAt(x, y, z);

            if (enforcePlayerDistance && !isFarFromPlayers(feet, player, minPlayerDist)) {
                continue;
            }

            return SafeLocationValidator.toLocation(feet);
        }
        return null;
    }

    /**
     * Find a Y inside the region's vertical range where the player can safely stand.
     *
     * <p>Scans the **entire** vertical range of the region (clamped to world height)
     * — no artificial scan limit. The region's own vertical span is the bound.
     *
     * <p>When sky exposure is required, the search starts from the top and scans
     * downward — sky-exposed locations are near the surface.
     *
     * <p>When sky exposure is not required (underground regions), the search
     * starts from a random Y and scans both downward and upward.
     */
    private int findSuitableY(@NotNull ResolvedRegion resolved, @NotNull World world, int x, int z,
                              @NotNull RegionBounds bounds, @NotNull SafeLocationValidator.Options options) {
        int minY = Math.max(bounds.minY(), world.getMinHeight());
        int maxY = Math.min(bounds.maxY(), world.getMaxHeight());
        if (minY > maxY) {
            return Integer.MIN_VALUE;
        }

        if (options.requireSkyExposure()) {
            // Search the entire range from the top downward — surface-first
            for (int y = maxY; y >= minY; y--) {
                if (isSafeStandingY(resolved, world, x, y, z, options)) {
                    return y;
                }
            }
            return Integer.MIN_VALUE;
        }

        // No sky exposure required: scan the entire range, random start
        int startY = selector.sampleY(bounds);

        // Search downward from startY
        for (int y = startY; y >= minY; y--) {
            if (isSafeStandingY(resolved, world, x, y, z, options)) {
                return y;
            }
        }
        // Then upward
        for (int y = startY + 1; y <= maxY; y++) {
            if (isSafeStandingY(resolved, world, x, y, z, options)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private boolean isSafeStandingY(@NotNull ResolvedRegion resolved, @NotNull World world, int x, int y, int z,
                                    @NotNull SafeLocationValidator.Options options) {
        // Quick WorldGuard vertical containment before touching blocks
        if (!resolved.contains(x, y, z) || !resolved.contains(x, y + 1, z)) {
            return false;
        }
        Block feet = world.getBlockAt(x, y, z);
        return validator.check(feet, resolved, options) == SafeLocationValidator.Result.OK;
    }

    /**
     * Check that no other player in the same world is within {@code minDist}
     * of the candidate feet block.
     */
    private boolean isFarFromPlayers(@NotNull Block feet, @NotNull Player teleporting, double minDist) {
        if (minDist <= 0) {
            return true;
        }
        double sq = minDist * minDist;
        double fx = feet.getX() + 0.5;
        double fz = feet.getZ() + 0.5;
        double fy = feet.getY();
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(teleporting)) {
                continue;
            }
            if (!other.getWorld().equals(feet.getWorld())) {
                continue;
            }
            Location o = other.getLocation();
            double dx = o.getX() - fx;
            double dy = o.getY() - fy;
            double dz = o.getZ() - fz;
            if (dx * dx + dy * dy + dz * dz <= sq) {
                return false;
            }
        }
        return true;
    }

    private @NotNull RegionBounds boundsFor(@NotNull ResolvedRegion resolved, @NotNull World world) {
        var min = resolved.minimumPoint();
        var max = resolved.maximumPoint();
        return new RegionBounds(
                min.getBlockX(), max.getBlockX(),
                min.getBlockZ(), max.getBlockZ(),
                min.getBlockY(), max.getBlockY(),
                world.getMinHeight(), world.getMaxHeight()
        );
    }

    /**
     * Set the yaw of {@code destination} so the player faces toward the
     * horizontal centre of the region's bounding box. Pitch is set to 0
     * (looking straight ahead).
     */
    private void faceTowardRegionCenter(@NotNull Location destination, @NotNull ResolvedRegion resolved) {
        var center = resolved.centerPoint();
        double dx = (center.getBlockX() + 0.5) - destination.getX();
        double dz = (center.getBlockZ() + 0.5) - destination.getZ();
        // Minecraft yaw: 0 = facing +Z, 90 = -X, etc.
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        destination.setYaw(yaw);
        destination.setPitch(0f);
    }
}