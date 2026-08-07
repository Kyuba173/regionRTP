package dev.kyuba.region_rtp;

import dev.kyuba.region_rtp.dungeon.ConfigDungeonRegionSource;
import dev.kyuba.region_rtp.dungeon.DungeonRegionSource;
import org.bukkit.plugin.java.JavaPlugin;

public final class RegionRTP extends JavaPlugin {

    private DungeonRegionSource dungeonRegionSource;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        /*
         * In production, StoryDungeon already knows which WorldGuard regions
         * are registered as dungeons.
         *
         * For this plugin(addon), config.yml acts as the source instead
         *
         * The teleport implementation should depend on DungeonRegionSource,
         * not directly on StoryDungeon internals.
         */
        dungeonRegionSource = new ConfigDungeonRegionSource(this);

        // TODO:
        // Implement and register the randomized dungeon spawn system.
    }

    public DungeonRegionSource dungeonRegions() {
        return dungeonRegionSource;
    }
}