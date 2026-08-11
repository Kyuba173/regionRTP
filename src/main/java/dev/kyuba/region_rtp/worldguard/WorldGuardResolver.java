package dev.kyuba.region_rtp.worldguard;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/**
 * Resolves WorldGuard regions from a {@link dev.kyuba.region_rtp.region.RtpRegion}.
 *
 * <p>Centralizes all WorldGuard lookups so the rest of the code never touches
 * the WorldGuard API directly, keeping the abstraction testable.
 */
public final class WorldGuardResolver {

    /**
     * Resolve the Bukkit world by name.
     *
     * @return the world, or {@code null} if it is not loaded
     */
    public @Nullable World world(@NotNull String worldName) {
        return Bukkit.getWorld(worldName);
    }

    /**
     * Get the WorldGuard {@link RegionManager} for a world.
     *
     * @return the region manager, or {@code null} if WorldGuard is unavailable for this world
     */
    public @Nullable RegionManager regionManager(@NotNull World world) {
        try {
            return WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get a named WorldGuard region.
     *
     * @return the region, or {@code null} if it does not exist
     */
    public @Nullable ProtectedRegion region(@NotNull RegionManager manager, @NotNull String regionId) {
        try {
            return manager.getRegion(regionId);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * WorldGuard containment check. Uses the region's own containment check
     * rather than assuming a simple cuboid bounding box.
     */
    public boolean contains(@NotNull ProtectedRegion region, int x, int y, int z) {
        try {
            return region.contains(BlockVector3.at(x, y, z));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Convenience: full resolution from world+region name to a {@link ResolvedRegion}.
     *
     * @return a resolved region, or {@code null} if the world or region is unavailable
     */
    public @Nullable ResolvedRegion resolve(@NotNull String worldName, @NotNull String regionId) {
        Objects.requireNonNull(worldName, "worldName");
        Objects.requireNonNull(regionId, "regionId");
        World world = world(worldName);
        if (world == null) {
            return null;
        }
        RegionManager manager = regionManager(world);
        if (manager == null) {
            return null;
        }
        ProtectedRegion region = region(manager, regionId);
        if (region == null) {
            return null;
        }
        return new ResolvedRegion(world, manager, region);
    }
}