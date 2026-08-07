package dev.kyuba.region_rtp.dungeon;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Development implementation of DungeonRegionSource.
 *
 * Production integration later replaces this with an adapter
 * backed directly by the main Dungeon plugin.
 */
public final class ConfigDungeonRegionSource implements DungeonRegionSource {

    private final JavaPlugin plugin;

    public ConfigDungeonRegionSource(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull Collection<DungeonRegion> regions() {
        ConfigurationSection section =
                plugin.getConfig().getConfigurationSection("dungeons");

        if (section == null) {
            return List.of();
        }

        List<DungeonRegion> regions = new ArrayList<>();

        for (String key : section.getKeys(false)) {
            ConfigurationSection dungeon = section.getConfigurationSection(key);

            if (dungeon == null) {
                continue;
            }

            String world = dungeon.getString("world");
            String region = dungeon.getString("region");

            if (world == null || world.isBlank()
                    || region == null || region.isBlank()) {
                continue;
            }

            regions.add(new DungeonRegion(world, region));
        }

        return List.copyOf(regions);
    }
}