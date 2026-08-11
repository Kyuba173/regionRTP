package dev.kyuba.region_rtp.spawn;

import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import dev.kyuba.region_rtp.config.SpawnConfig;

class CandidateSelectorTest {

    private SpawnConfig config(int attempts, double edge, double playerDist) {
        return SpawnConfig.sanitize(attempts, edge, playerDist, new java.util.ArrayList<>());
    }

    @Test
    void isEdgeNearTrueAtBoundary() {
        SpawnConfig cfg = config(10, 5, 15);
        CandidateSelector s = new CandidateSelector(cfg, RandomGenerator.getDefault());
        RegionBounds b = new RegionBounds(0, 100, 0, 100, 0, 64, -64, 320);
        // Corner = distance 0 to both edges
        assertTrue(s.isEdgeNear(0, 0, b));
        assertTrue(s.isEdgeNear(100, 100, b));
        assertTrue(s.isEdgeNear(50, 2, b));
    }

    @Test
    void isEdgeNearFalseInCenter() {
        SpawnConfig cfg = config(10, 5, 15);
        CandidateSelector s = new CandidateSelector(cfg, RandomGenerator.getDefault());
        RegionBounds b = new RegionBounds(0, 100, 0, 100, 0, 64, -64, 320);
        assertFalse(s.isEdgeNear(50, 50, b));
        assertFalse(s.isEdgeNear(50, 10, b));
    }

    @Test
    void isEdgeNearExactlyAtEdgeDistance() {
        SpawnConfig cfg = config(10, 5, 15);
        CandidateSelector s = new CandidateSelector(cfg, RandomGenerator.getDefault());
        RegionBounds b = new RegionBounds(0, 100, 0, 100, 0, 64, -64, 320);
        // distance = 5 exactly -> accepted (<=)
        assertTrue(s.isEdgeNear(5, 50, b));
        assertTrue(s.isEdgeNear(95, 50, b));
        // distance = 6 -> rejected
        assertFalse(s.isEdgeNear(6, 50, b));
    }

    @Test
    void minDistanceToBoundaryCorrect() {
        // bounds 0..100, point (5, 50): min(5, 95, 50, 50) = 5
        assertEquals(5.0, CandidateSelector.minDistanceToBoundary(5, 50, 0, 100, 0, 100));
        // corner
        assertEquals(0.0, CandidateSelector.minDistanceToBoundary(0, 0, 0, 100, 0, 100));
        // center
        assertEquals(50.0, CandidateSelector.minDistanceToBoundary(50, 50, 0, 100, 0, 100));
    }

    @Test
    void sampleXWithinBounds() {
        SpawnConfig cfg = config(10, 5, 15);
        CandidateSelector s = new CandidateSelector(cfg, RandomGenerator.getDefault());
        RegionBounds b = new RegionBounds(10, 20, 30, 40, 0, 64, -64, 320);
        for (int i = 0; i < 1000; i++) {
            int x = s.sampleX(b);
            assertTrue(x >= 10 && x <= 20, "x out of bounds: " + x);
            int z = s.sampleZ(b);
            assertTrue(z >= 30 && z <= 40, "z out of bounds: " + z);
        }
    }

    @Test
    void sampleYClampedToWorldHeight() {
        SpawnConfig cfg = config(10, 5, 15);
        CandidateSelector s = new CandidateSelector(cfg, RandomGenerator.getDefault());
        // region y 0..64, world -64..320
        RegionBounds b = new RegionBounds(0, 100, 0, 100, 0, 64, -64, 320);
        for (int i = 0; i < 1000; i++) {
            int y = s.sampleY(b);
            assertTrue(y >= 0 && y <= 64, "y out of bounds: " + y);
        }
    }

    @Test
    void sampleYWhenRegionExceedsWorldHeightClamps() {
        SpawnConfig cfg = config(10, 5, 15);
        CandidateSelector s = new CandidateSelector(cfg, RandomGenerator.getDefault());
        // region y -100..500, world 0..255
        RegionBounds b = new RegionBounds(0, 100, 0, 100, -100, 500, 0, 255);
        for (int i = 0; i < 1000; i++) {
            int y = s.sampleY(b);
            assertTrue(y >= 0 && y <= 255, "y out of world bounds: " + y);
        }
    }

    @Test
    void sampleYWhenRangeInvalidReturnsMin() {
        SpawnConfig cfg = config(10, 5, 15);
        CandidateSelector s = new CandidateSelector(cfg, RandomGenerator.getDefault());
        // region y 300..400, world -64..64 -> minY clamped = max(300,-64)=300, maxY = min(400,64)=64
        RegionBounds b = new RegionBounds(0, 100, 0, 100, 300, 400, -64, 64);
        int y = s.sampleY(b);
        assertEquals(300, y);
    }

    @Test
    void fallbackAttemptsAtLeast1() {
        SpawnConfig cfg = config(3, 5, 15);
        CandidateSelector s = new CandidateSelector(cfg, RandomGenerator.getDefault());
        assertEquals(1, s.fallbackAttempts());

        SpawnConfig cfg2 = config(50, 5, 15);
        CandidateSelector s2 = new CandidateSelector(cfg2, RandomGenerator.getDefault());
        assertEquals(25, s2.fallbackAttempts());
    }

    @Test
    void edgeDistanceZeroOnlyBoundaryAccepted() {
        SpawnConfig cfg = config(10, 0, 15);
        CandidateSelector s = new CandidateSelector(cfg, RandomGenerator.getDefault());
        RegionBounds b = new RegionBounds(0, 100, 0, 100, 0, 64, -64, 320);
        // only exact boundary (distance 0)
        assertTrue(s.isEdgeNear(0, 50, b));
        assertTrue(s.isEdgeNear(100, 50, b));
        assertTrue(s.isEdgeNear(50, 0, b));
        assertTrue(s.isEdgeNear(50, 100, b));
        assertFalse(s.isEdgeNear(1, 50, b));
    }
}