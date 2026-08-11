package dev.kyuba.region_rtp.region;

import org.jetbrains.annotations.NotNull;

/**
 * Minimal representation of a region.
 *
 * <p>Stores the logical config id, the world name, the WorldGuard region id,
 * and per-region spawn options.
 *
 * <p>The id is the key under {@code regions} in {@code config.yml}
 * (e.g. {@code "castle"} for {@code regions.castle}).
 *
 * @param id                      logical config id used by commands and tab completion
 * @param worldName               Bukkit/Paper world name
 * @param regionId                WorldGuard region id
 * @param requireSkyExposure      when {@code true}, spawn candidates must have no
 *                                solid blocks above them up to the sky (avoids
 *                                spawning inside caves/enclosed ceilings)
 * @param allowWater               when {@code true}, water is an acceptable feet/head
 *                                block; when {@code false}, water is treated as
 *                                dangerous (default behaviour)
 * @param teleportDelaySeconds     seconds the player must stand still before the
 *                                teleport is applied (0 = instant)
 * @param cooldownSeconds          seconds the player must wait after a successful
 *                                teleport before using this region again
 *                                (0 = no cooldown)
 */
public record RtpRegion(
        @NotNull String id,
        @NotNull String worldName,
        @NotNull String regionId,
        boolean requireSkyExposure,
        boolean allowWater,
        int teleportDelaySeconds,
        int cooldownSeconds
) {
}