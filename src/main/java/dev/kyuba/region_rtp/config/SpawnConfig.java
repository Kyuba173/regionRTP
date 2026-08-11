package dev.kyuba.region_rtp.config;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Parsed and validated spawn-related configuration.
 *
 * <p>Sanitizes values at load time so the rest of the code can rely on
 * sensible bounds without re-checking everywhere.
 */
public final class SpawnConfig {

    public static final int MIN_ATTEMPTS = 1;
    public static final int MAX_ATTEMPTS = 500;
    public static final int DEFAULT_ATTEMPTS = 50;

    public static final double MIN_EDGE_DISTANCE = 0.0;
    public static final double MAX_EDGE_DISTANCE = 256.0;
    public static final double DEFAULT_EDGE_DISTANCE = 5.0;

    public static final double MIN_PLAYER_DISTANCE = 0.0;
    public static final double MAX_PLAYER_DISTANCE = 1024.0;
    public static final double DEFAULT_PLAYER_DISTANCE = 15.0;

    private final int attempts;
    private final double edgeDistance;
    private final double minimumPlayerDistance;

    private SpawnConfig(int attempts, double edgeDistance, double minimumPlayerDistance) {
        this.attempts = attempts;
        this.edgeDistance = edgeDistance;
        this.minimumPlayerDistance = minimumPlayerDistance;
    }

    public int attempts() {
        return attempts;
    }

    /**
     * Maximum distance from the horizontal region boundary for a valid candidate.
     */
    public double edgeDistance() {
        return edgeDistance;
    }

    public double minimumPlayerDistance() {
        return minimumPlayerDistance;
    }

    /**
     * Parse the {@code spawn} section from the plugin config, clamping bad values
     * and logging warnings.
     */
    public static @NotNull SpawnConfig load(@NotNull JavaPlugin plugin) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("spawn");
        int attempts = DEFAULT_ATTEMPTS;
        double edgeDistance = DEFAULT_EDGE_DISTANCE;
        double playerDistance = DEFAULT_PLAYER_DISTANCE;

        if (section != null) {
            List<String> warnings = new ArrayList<>();
            int[] sanitized = new int[1];
            double[] edges = new double[1];
            double[] players = new double[1];

            sanitized[0] = sanitizeAttempts(section.getInt("attempts", DEFAULT_ATTEMPTS), warnings);
            edges[0] = sanitizeEdgeDistance(section.getDouble("edge-distance", DEFAULT_EDGE_DISTANCE), warnings);
            players[0] = sanitizePlayerDistance(section.getDouble("minimum-player-distance", DEFAULT_PLAYER_DISTANCE), warnings);

            attempts = sanitized[0];
            edgeDistance = edges[0];
            playerDistance = players[0];

            for (String w : warnings) {
                plugin.getLogger().warning(w);
            }
        } else {
            plugin.getLogger().info("No 'spawn' section in config.yml; using defaults.");
        }
        return new SpawnConfig(attempts, edgeDistance, playerDistance);
    }

    /**
     * Testable factory that sanitizes raw values and collects warnings.
     */
    public static @NotNull SpawnConfig sanitize(int rawAttempts, double rawEdge, double rawPlayer,
                                                 @NotNull List<String> warnings) {
        return new SpawnConfig(
                sanitizeAttempts(rawAttempts, warnings),
                sanitizeEdgeDistance(rawEdge, warnings),
                sanitizePlayerDistance(rawPlayer, warnings)
        );
    }

    static int sanitizeAttempts(int raw, @NotNull List<String> warnings) {
        if (raw < MIN_ATTEMPTS) {
            warnings.add("spawn.attempts=" + raw + " is below minimum " + MIN_ATTEMPTS
                    + "; using " + DEFAULT_ATTEMPTS + ".");
            return DEFAULT_ATTEMPTS;
        }
        if (raw > MAX_ATTEMPTS) {
            warnings.add("spawn.attempts=" + raw + " is above maximum " + MAX_ATTEMPTS
                    + "; clamping to " + MAX_ATTEMPTS + ".");
            return MAX_ATTEMPTS;
        }
        return raw;
    }

    static double sanitizeEdgeDistance(double raw, @NotNull List<String> warnings) {
        if (raw < MIN_EDGE_DISTANCE) {
            warnings.add("spawn.edge-distance=" + raw + " is below minimum " + MIN_EDGE_DISTANCE
                    + "; using " + DEFAULT_EDGE_DISTANCE + ".");
            return DEFAULT_EDGE_DISTANCE;
        }
        if (raw > MAX_EDGE_DISTANCE) {
            warnings.add("spawn.edge-distance=" + raw + " is above maximum " + MAX_EDGE_DISTANCE
                    + "; clamping to " + MAX_EDGE_DISTANCE + ".");
            return MAX_EDGE_DISTANCE;
        }
        return raw;
    }

    static double sanitizePlayerDistance(double raw, @NotNull List<String> warnings) {
        if (raw < MIN_PLAYER_DISTANCE) {
            warnings.add("spawn.minimum-player-distance=" + raw + " is below minimum "
                    + MIN_PLAYER_DISTANCE + "; using " + DEFAULT_PLAYER_DISTANCE + ".");
            return DEFAULT_PLAYER_DISTANCE;
        }
        if (raw > MAX_PLAYER_DISTANCE) {
            warnings.add("spawn.minimum-player-distance=" + raw + " is above maximum "
                    + MAX_PLAYER_DISTANCE + "; clamping to " + MAX_PLAYER_DISTANCE + ".");
            return MAX_PLAYER_DISTANCE;
        }
        return raw;
    }
}