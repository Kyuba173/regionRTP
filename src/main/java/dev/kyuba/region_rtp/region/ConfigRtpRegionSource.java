package dev.kyuba.region_rtp.region;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Development implementation of {@link RtpRegionSource} backed by
 * the plugin's {@code config.yml}.
 *
 * <p>Production integration later replaces this with an adapter
 * backed directly by the plugin.
 */
public final class ConfigRtpRegionSource implements RtpRegionSource {

    private final JavaPlugin plugin;
    private volatile Map<String, RtpRegion> regions = Map.of();

    public ConfigRtpRegionSource(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * Re-read the config. Safe to call on reload.
     */
    public void reload() {
        ConfigurationSection section =
                plugin.getConfig().getConfigurationSection("regions");

        Map<String, RtpRegion> map = new HashMap<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection region = section.getConfigurationSection(key);
                if (region == null) {
                    continue;
                }
                String world = region.getString("world");
                String wgRegion = region.getString("region");
                if (world == null || world.isBlank()
                        || wgRegion == null || wgRegion.isBlank()) {
                    plugin.getLogger().warning(
                            "Skipping region '" + key + "': missing or blank world/region.");
                    continue;
                }
                boolean requireSkyExposure = region.getBoolean("require-sky-exposure", true);
                boolean allowWater = region.getBoolean("allow-water", false);
                int teleportDelay = region.getInt("teleport-delay-seconds", 0);
                if (teleportDelay < 0) {
                    plugin.getLogger().warning(
                            "Region '" + key + "': teleport-delay-seconds=" + teleportDelay
                                    + " is negative; using 0.");
                    teleportDelay = 0;
                }
                if (teleportDelay > 60) {
                    plugin.getLogger().warning(
                            "Region '" + key + "': teleport-delay-seconds=" + teleportDelay
                                    + " is above 60; clamping to 60.");
                    teleportDelay = 60;
                }

                int cooldown = region.getInt("cooldown-seconds", 0);
                if (cooldown < 0) {
                    plugin.getLogger().warning(
                            "Region '" + key + "': cooldown-seconds=" + cooldown
                                    + " is negative; using 0.");
                    cooldown = 0;
                }
                if (cooldown > 3600) {
                    plugin.getLogger().warning(
                            "Region '" + key + "': cooldown-seconds=" + cooldown
                                    + " is above 3600; clamping to 3600.");
                    cooldown = 3600;
                }

                map.put(key, new RtpRegion(
                        key, world, wgRegion,
                        requireSkyExposure, allowWater, teleportDelay, cooldown));
            }
        }
        this.regions = Map.copyOf(map);
    }

    @Override
    public @NotNull Collection<RtpRegion> regions() {
        return regions.values();
    }

    @Override
    public @Nullable RtpRegion byId(@NotNull String id) {
        return regions.get(id);
    }
}