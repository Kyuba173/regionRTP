package dev.kyuba.region_rtp.config;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class SpawnConfigTest {

    @Test
    void validValuesPreserved() {
        List<String> warnings = new ArrayList<>();
        SpawnConfig cfg = SpawnConfig.sanitize(50, 5, 15, warnings);
        assertEquals(50, cfg.attempts());
        assertEquals(5.0, cfg.edgeDistance());
        assertEquals(15.0, cfg.minimumPlayerDistance());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void attemptsTooLowUsesDefault() {
        List<String> warnings = new ArrayList<>();
        SpawnConfig cfg = SpawnConfig.sanitize(0, 5, 15, warnings);
        assertEquals(SpawnConfig.DEFAULT_ATTEMPTS, cfg.attempts());
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("attempts"));
    }

    @Test
    void attemptsTooHighClamped() {
        List<String> warnings = new ArrayList<>();
        SpawnConfig cfg = SpawnConfig.sanitize(9999, 5, 15, warnings);
        assertEquals(SpawnConfig.MAX_ATTEMPTS, cfg.attempts());
        assertEquals(1, warnings.size());
    }

    @Test
    void edgeDistanceNegativeUsesDefault() {
        List<String> warnings = new ArrayList<>();
        SpawnConfig cfg = SpawnConfig.sanitize(50, -1, 15, warnings);
        assertEquals(SpawnConfig.DEFAULT_EDGE_DISTANCE, cfg.edgeDistance());
        assertEquals(1, warnings.size());
    }

    @Test
    void edgeDistanceTooHighClamped() {
        List<String> warnings = new ArrayList<>();
        SpawnConfig cfg = SpawnConfig.sanitize(50, 10000, 15, warnings);
        assertEquals(SpawnConfig.MAX_EDGE_DISTANCE, cfg.edgeDistance());
    }

    @Test
    void playerDistanceNegativeUsesDefault() {
        List<String> warnings = new ArrayList<>();
        SpawnConfig cfg = SpawnConfig.sanitize(50, 5, -5, warnings);
        assertEquals(SpawnConfig.DEFAULT_PLAYER_DISTANCE, cfg.minimumPlayerDistance());
    }

    @Test
    void playerDistanceTooHighClamped() {
        List<String> warnings = new ArrayList<>();
        SpawnConfig cfg = SpawnConfig.sanitize(50, 5, 99999, warnings);
        assertEquals(SpawnConfig.MAX_PLAYER_DISTANCE, cfg.minimumPlayerDistance());
    }

    @Test
    void boundaryValuesAccepted() {
        List<String> warnings = new ArrayList<>();
        SpawnConfig cfg = SpawnConfig.sanitize(
                SpawnConfig.MIN_ATTEMPTS,
                SpawnConfig.MIN_EDGE_DISTANCE,
                SpawnConfig.MIN_PLAYER_DISTANCE,
                warnings);
        assertTrue(warnings.isEmpty());
        assertEquals(SpawnConfig.MIN_ATTEMPTS, cfg.attempts());

        List<String> warnings2 = new ArrayList<>();
        SpawnConfig cfg2 = SpawnConfig.sanitize(
                SpawnConfig.MAX_ATTEMPTS,
                SpawnConfig.MAX_EDGE_DISTANCE,
                SpawnConfig.MAX_PLAYER_DISTANCE,
                warnings2);
        assertTrue(warnings2.isEmpty());
    }
}