package dev.kyuba.region_rtp;

import java.util.random.RandomGenerator;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import dev.kyuba.region_rtp.command.AdminCommand;
import dev.kyuba.region_rtp.command.RegionRtpCommand;
import dev.kyuba.region_rtp.config.SpawnConfig;
import dev.kyuba.region_rtp.region.ConfigRtpRegionSource;
import dev.kyuba.region_rtp.region.RtpRegionSource;
import dev.kyuba.region_rtp.spawn.TeleportService;
import dev.kyuba.region_rtp.worldguard.WorldGuardResolver;

public final class RegionRTP extends JavaPlugin {

    private final WorldGuardResolver worldGuardResolver = new WorldGuardResolver();
    private ConfigRtpRegionSource regionSource;
    private SpawnConfig spawnConfig;
    private TeleportService teleportService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        regionSource = new ConfigRtpRegionSource(this);
        spawnConfig = SpawnConfig.load(this);
        RandomGenerator random = RandomGenerator.getDefault();
        teleportService = new TeleportService(this, worldGuardResolver, spawnConfig, random);

        registerCommand();
        registerAdminCommand();
    }

    private void registerCommand() {
        PluginCommand command = getCommand("regionrtp");
        if (command == null) {
            getLogger().severe("Could not find 'regionrtp' command in plugin.yml — plugin will not function.");
            return;
        }
        command.setExecutor(new RegionRtpCommand(regionSource, teleportService, this::reload));
    }

    private void registerAdminCommand() {
        PluginCommand command = getCommand("rrtp");
        if (command == null) {
            getLogger().severe("Could not find 'rrtp' command in plugin.yml — admin commands will not function.");
            return;
        }
        command.setExecutor(new AdminCommand(this, regionSource, this::reload));
    }

    /**
     * Reload config, region source and spawn config.
     */
    public void reload() {
        reloadConfig();
        regionSource.reload();
        spawnConfig = SpawnConfig.load(this);
        RandomGenerator random = RandomGenerator.getDefault();
        teleportService = new TeleportService(this, worldGuardResolver, spawnConfig, random);
        registerCommand();
    }

    public RtpRegionSource rtpRegions() {
        return regionSource;
    }

    public SpawnConfig spawnConfig() {
        return spawnConfig;
    }
}