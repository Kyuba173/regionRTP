package dev.kyuba.region_rtp.dungeon;

import org.jetbrains.annotations.NotNull;
import java.util.Collection;

/**
 * Provides the dungeon regions which may be used by the spawn system.
 *
 * The Dungeon plugin already manages registered WorldGuard
 * dungeon regions.
 *
 * This interface exists so the spawn logic does not need to know
 * how StoryDungeon stores or manages those regions.
 */
public interface DungeonRegionSource {

    @NotNull
    Collection<DungeonRegion> regions();
}