package dev.kyuba.region_rtp.region;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides the regions which may be used by the spawn system.
 *
 * <p>The plugin already manages registered WorldGuard
 * regions. This interface exists so the spawn logic does not
 * need to know how those regions are stored or managed.
 */
public interface RtpRegionSource {

    @NotNull
    Collection<RtpRegion> regions();

    /**
     * Resolve a region by its logical config id.
     *
     * @param id logical id (e.g. {@code "castle"})
     * @return the matching region, or {@code null} if no such region is configured
     */
    @Nullable
    RtpRegion byId(@NotNull String id);
}