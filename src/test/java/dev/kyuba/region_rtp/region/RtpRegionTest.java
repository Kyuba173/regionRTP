package dev.kyuba.region_rtp.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class RtpRegionTest {

    @Test
    void recordAccessors() {
        RtpRegion r = new RtpRegion("castle", "world", "castle_region", true, false, 3, 10);
        assertEquals("castle", r.id());
        assertEquals("world", r.worldName());
        assertEquals("castle_region", r.regionId());
        assertTrue(r.requireSkyExposure());
        assertFalse(r.allowWater());
        assertEquals(3, r.teleportDelaySeconds());
        assertEquals(10, r.cooldownSeconds());
    }

    @Test
    void equalsHashCode() {
        RtpRegion a = new RtpRegion("castle", "world", "castle_region", true, false, 0, 0);
        RtpRegion b = new RtpRegion("castle", "world", "castle_region", true, false, 0, 0);
        RtpRegion c = new RtpRegion("crypt", "world", "crypt_region", false, true, 5, 30);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    void differentOptionsNotEqual() {
        RtpRegion a = new RtpRegion("castle", "world", "castle_region", true, false, 0, 0);
        RtpRegion b = new RtpRegion("castle", "world", "castle_region", false, true, 2, 5);
        assertNotEquals(a, b);
    }

    @Test
    void differentDelayNotEqual() {
        RtpRegion a = new RtpRegion("castle", "world", "castle_region", true, false, 0, 0);
        RtpRegion b = new RtpRegion("castle", "world", "castle_region", true, false, 3, 0);
        assertNotEquals(a, b);
    }

    @Test
    void differentCooldownNotEqual() {
        RtpRegion a = new RtpRegion("castle", "world", "castle_region", true, false, 0, 0);
        RtpRegion b = new RtpRegion("castle", "world", "castle_region", true, false, 0, 30);
        assertNotEquals(a, b);
    }
}