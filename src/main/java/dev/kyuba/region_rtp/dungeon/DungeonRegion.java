package dev.kyuba.region_rtp.dungeon;

import org.jetbrains.annotations.NotNull;

/**
 * Minimal representation of a dungeon region.
 *
 * The Dungeon plugin internally identifies dungeon regions using a world
 * and a WorldGuard region id.
 *
 */
public record DungeonRegion(
        @NotNull String worldName,
        @NotNull String regionId
) {
}