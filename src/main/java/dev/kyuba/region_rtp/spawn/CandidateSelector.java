package dev.kyuba.region_rtp.spawn;

import java.util.random.RandomGenerator;

import org.jetbrains.annotations.NotNull;

import dev.kyuba.region_rtp.config.SpawnConfig;

/**
 * Pure decision/geometry logic for biased candidate sampling.
 *
 * <p>This class is intentionally free of Bukkit/WorldGuard types so it can be
 * unit-tested without a server. It operates on an abstract {@link RegionBounds}
 * and produces raw block coordinates that the caller then validates against
 * the actual WorldGuard region containment check.
 *
 * <h2>Edge-bias algorithm</h2>
 * <p>"Near the region edge" is defined as a band of width {@code edgeDistance}
 * inside the region's horizontal bounding box boundary. Concretely, a
 * candidate (x, z) is considered edge-near if the distance from (x, z) to the
 * nearest horizontal boundary of the bounding box is ≤ {@code edgeDistance}.
 *
 * <p>The sampling strategy:
 * <ol>
 *   <li>Pick a random point uniformly from the bounding box.</li>
 *   <li>If it falls within the edge band, accept it.</li>
 *   <li>Otherwise, reject and resample (bounded by {@code attempts}).</li>
 * </ol>
 * <p>This produces a band of candidates near the boundary without enumerating
 * every block. For non-cuboid regions the final containment check is still
 * delegated to WorldGuard — this class only narrows the search area.
 */
public final class CandidateSelector {

    private final SpawnConfig config;
    private final RandomGenerator random;

    public CandidateSelector(@NotNull SpawnConfig config, @NotNull RandomGenerator random) {
        this.config = config;
        this.random = random;
    }

    /**
     * Determine whether a sampled (x, z) lies within the edge band of the
     * given bounds.
     *
     * @param x      sampled block x
     * @param z      sampled block z
     * @param bounds region bounding box
     * @return {@code true} if the point is within {@code edgeDistance} of the
     *         horizontal boundary of the bounding box
     */
    public boolean isEdgeNear(int x, int z, @NotNull RegionBounds bounds) {
        double edge = config.edgeDistance();
        int minX = bounds.minX();
        int maxX = bounds.maxX();
        int minZ = bounds.minZ();
        int maxZ = bounds.maxZ();

        double distToEdge = minDistanceToBoundary(x, z, minX, maxX, minZ, maxZ);
        return distToEdge <= edge;
    }

    /**
     * Minimum horizontal distance from (x, z) to the bounding-box boundary.
     */
    public static double minDistanceToBoundary(int x, int z, int minX, int maxX, int minZ, int maxZ) {
        double dx = Math.min(x - minX, maxX - x);
        double dz = Math.min(z - minZ, maxZ - z);
        return Math.min(dx, dz);
    }

    /**
     * Sample a random x within the bounds.
     */
    public int sampleX(@NotNull RegionBounds bounds) {
        return random.nextInt(bounds.minX(), bounds.maxX() + 1);
    }

    /**
     * Sample a random z within the bounds.
     */
    public int sampleZ(@NotNull RegionBounds bounds) {
        return random.nextInt(bounds.minZ(), bounds.maxZ() + 1);
    }

    /**
     * Sample a random y within the bounds' vertical range, clamped to the
     * world's valid height.
     */
    public int sampleY(@NotNull RegionBounds bounds) {
        int minY = Math.max(bounds.minY(), bounds.worldMinY());
        int maxY = Math.min(bounds.maxY(), bounds.worldMaxY());
        if (minY > maxY) {
            return minY;
        }
        return random.nextInt(minY, maxY + 1);
    }

    /**
     * Maximum candidate attempts.
     */
    public int maxAttempts() {
        return config.attempts();
    }

    /**
     * Maximum number of fallback attempts that relax the player-distance
     * preference once all preferred candidates fail. Bounded so the overall
     * search remains capped.
     */
    public int fallbackAttempts() {
        return Math.max(1, config.attempts() / 2);
    }

    /**
     * Minimum player distance preference.
     */
    public double minimumPlayerDistance() {
        return config.minimumPlayerDistance();
    }
}