package dev.kyuba.region_rtp.spawn;

/**
 * Immutable bounds describing the area from which candidates are sampled.
 *
 * <p>{@code minX/maxX/minZ/maxZ} describe the horizontal bounding box of the
 * WorldGuard region. {@code minY/maxY} describe the region's vertical range.
 * {@code worldMinY/worldMaxY} describe the world's valid build height so the
 * sampler can clamp Y values.
 *
 * <p>This is a pure data carrier with no Bukkit dependency, so it can be used
 * in unit tests.
 */
public record RegionBounds(
        int minX,
        int maxX,
        int minZ,
        int maxZ,
        int minY,
        int maxY,
        int worldMinY,
        int worldMaxY
) {
}