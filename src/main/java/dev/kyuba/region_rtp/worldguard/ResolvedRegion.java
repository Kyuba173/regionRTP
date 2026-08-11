package dev.kyuba.region_rtp.worldguard;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/**
 * A WorldGuard region resolved together with its owning Bukkit world.
 */
public record ResolvedRegion(
        @NotNull World world,
        @NotNull RegionManager regionManager,
        @NotNull ProtectedRegion region
) {

    /**
     * Minimum block coordinate of the region's bounding box.
     */
    public @NotNull BlockVector3 minimumPoint() {
        return region.getMinimumPoint();
    }

    /**
     * Maximum block coordinate of the region's bounding box.
     */
    public @NotNull BlockVector3 maximumPoint() {
        return region.getMaximumPoint();
    }

    /**
     * Horizontal center of the region's bounding box (X, Z).
     *
     * @return a {@link BlockVector3} whose X and Z are the bounding-box centre
     *         and whose Y is the vertical centre
     */
    public @NotNull BlockVector3 centerPoint() {
        BlockVector3 min = minimumPoint();
        BlockVector3 max = maximumPoint();
        return BlockVector3.at(
                (min.getBlockX() + max.getBlockX()) / 2,
                (min.getBlockY() + max.getBlockY()) / 2,
                (min.getBlockZ() + max.getBlockZ()) / 2
        );
    }

    /**
     * WorldGuard containment test for a block coordinate.
     */
    public boolean contains(int x, int y, int z) {
        return region.contains(BlockVector3.at(x, y, z));
    }
}