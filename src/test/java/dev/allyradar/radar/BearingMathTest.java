package dev.allyradar.radar;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BearingMathTest {
    private static final double DELTA = 0.001;

    @Test
    @DisplayName("Viewer facing South (yaw=0) calculates correct relative bearings and arrows")
    void viewerFacingSouth() {
        // Target South (0, 10) -> Directly ahead (0 deg)
        double bearing = RadarMath.relativeBearing(0, 0, 0f, 0, 10);
        assertEquals(0.0, bearing, DELTA);
        assertEquals("↑", RadarMath.direction(bearing));

        // Target West (-10, 0) -> Right (90 deg)
        bearing = RadarMath.relativeBearing(0, 0, 0f, -10, 0);
        assertEquals(90.0, bearing, DELTA);
        assertEquals("→", RadarMath.direction(bearing));

        // Target North (0, -10) -> Behind (180 or -180 deg)
        bearing = RadarMath.relativeBearing(0, 0, 0f, 0, -10);
        assertTrue(Math.abs(bearing) == 180.0 || Math.abs(bearing - 180.0) < DELTA || Math.abs(bearing + 180.0) < DELTA);
        assertEquals("↓", RadarMath.direction(bearing));

        // Target East (10, 0) -> Left (-90 deg)
        bearing = RadarMath.relativeBearing(0, 0, 0f, 10, 0);
        assertEquals(-90.0, bearing, DELTA);
        assertEquals("←", RadarMath.direction(bearing));

        // Target South-West (-10, 10) -> Ahead-Right (45 deg)
        bearing = RadarMath.relativeBearing(0, 0, 0f, -10, 10);
        assertEquals(45.0, bearing, DELTA);
        assertEquals("↗", RadarMath.direction(bearing));

        // Target North-West (-10, -10) -> Behind-Right (135 deg)
        bearing = RadarMath.relativeBearing(0, 0, 0f, -10, -10);
        assertEquals(135.0, bearing, DELTA);
        assertEquals("↘", RadarMath.direction(bearing));

        // Target North-East (10, -10) -> Behind-Left (-135 deg)
        bearing = RadarMath.relativeBearing(0, 0, 0f, 10, -10);
        assertEquals(-135.0, bearing, DELTA);
        assertEquals("↙", RadarMath.direction(bearing));

        // Target South-East (10, 10) -> Ahead-Left (-45 deg)
        bearing = RadarMath.relativeBearing(0, 0, 0f, 10, 10);
        assertEquals(-45.0, bearing, DELTA);
        assertEquals("↖", RadarMath.direction(bearing));
    }

    @Test
    @DisplayName("Viewer facing North (yaw=180) calculates correct relative bearings and arrows")
    void viewerFacingNorth() {
        // Target North (0, -10) -> Directly ahead (0 deg)
        double bearing = RadarMath.relativeBearing(0, 0, 180f, 0, -10);
        assertEquals(0.0, bearing, DELTA);
        assertEquals("↑", RadarMath.direction(bearing));

        // Target East (10, 0) -> Right (90 deg)
        bearing = RadarMath.relativeBearing(0, 0, 180f, 10, 0);
        assertEquals(90.0, bearing, DELTA);
        assertEquals("→", RadarMath.direction(bearing));

        // Target South (0, 10) -> Behind (180 or -180 deg)
        bearing = RadarMath.relativeBearing(0, 0, 180f, 0, 10);
        assertTrue(Math.abs(bearing) == 180.0 || Math.abs(bearing - 180.0) < DELTA || Math.abs(bearing + 180.0) < DELTA);
        assertEquals("↓", RadarMath.direction(bearing));

        // Target West (-10, 0) -> Left (-90 deg)
        bearing = RadarMath.relativeBearing(0, 0, 180f, -10, 0);
        assertEquals(-90.0, bearing, DELTA);
        assertEquals("←", RadarMath.direction(bearing));

        // Target North-East (10, -10) -> Ahead-Right (45 deg)
        bearing = RadarMath.relativeBearing(0, 0, 180f, 10, -10);
        assertEquals(45.0, bearing, DELTA);
        assertEquals("↗", RadarMath.direction(bearing));

        // Target South-East (10, 10) -> Behind-Right (135 deg)
        bearing = RadarMath.relativeBearing(0, 0, 180f, 10, 10);
        assertEquals(135.0, bearing, DELTA);
        assertEquals("↘", RadarMath.direction(bearing));

        // Target South-West (-10, 10) -> Behind-Left (-135 deg)
        bearing = RadarMath.relativeBearing(0, 0, 180f, -10, 10);
        assertEquals(-135.0, bearing, DELTA);
        assertEquals("↙", RadarMath.direction(bearing));

        // Target North-West (-10, -10) -> Ahead-Left (-45 deg)
        bearing = RadarMath.relativeBearing(0, 0, 180f, -10, -10);
        assertEquals(-45.0, bearing, DELTA);
        assertEquals("↖", RadarMath.direction(bearing));
    }

    @Test
    @DisplayName("Viewer facing East (yaw=-90) calculates correct relative bearings and arrows")
    void viewerFacingEast() {
        // Target East (10, 0) -> Ahead
        double bearing = RadarMath.relativeBearing(0, 0, -90f, 10, 0);
        assertEquals(0.0, bearing, DELTA);
        assertEquals("↑", RadarMath.direction(bearing));

        // Target South (0, 10) -> Right
        bearing = RadarMath.relativeBearing(0, 0, -90f, 0, 10);
        assertEquals(90.0, bearing, DELTA);
        assertEquals("→", RadarMath.direction(bearing));

        // Target West (-10, 0) -> Behind
        bearing = RadarMath.relativeBearing(0, 0, -90f, -10, 0);
        assertTrue(Math.abs(bearing) == 180.0 || Math.abs(bearing - 180.0) < DELTA || Math.abs(bearing + 180.0) < DELTA);
        assertEquals("↓", RadarMath.direction(bearing));

        // Target North (0, -10) -> Left
        bearing = RadarMath.relativeBearing(0, 0, -90f, 0, -10);
        assertEquals(-90.0, bearing, DELTA);
        assertEquals("←", RadarMath.direction(bearing));
    }

    @Test
    @DisplayName("Viewer facing West (yaw=90) calculates correct relative bearings and arrows")
    void viewerFacingWest() {
        // Target West (-10, 0) -> Ahead
        double bearing = RadarMath.relativeBearing(0, 0, 90f, -10, 0);
        assertEquals(0.0, bearing, DELTA);
        assertEquals("↑", RadarMath.direction(bearing));

        // Target North (0, -10) -> Right
        bearing = RadarMath.relativeBearing(0, 0, 90f, 0, -10);
        assertEquals(90.0, bearing, DELTA);
        assertEquals("→", RadarMath.direction(bearing));

        // Target East (10, 0) -> Behind
        bearing = RadarMath.relativeBearing(0, 0, 90f, 10, 0);
        assertTrue(Math.abs(bearing) == 180.0 || Math.abs(bearing - 180.0) < DELTA || Math.abs(bearing + 180.0) < DELTA);
        assertEquals("↓", RadarMath.direction(bearing));

        // Target South (0, 10) -> Left
        bearing = RadarMath.relativeBearing(0, 0, 90f, 0, 10);
        assertEquals(-90.0, bearing, DELTA);
        assertEquals("←", RadarMath.direction(bearing));
    }

    @ParameterizedTest(name = "Bearing {0} maps to arrow {1}")
    @CsvSource({
            "0.0, ↑",
            "-22.5, ↑",
            "22.4, ↑",
            "22.5, ↗",
            "45.0, ↗",
            "67.4, ↗",
            "67.5, →",
            "90.0, →",
            "112.4, →",
            "112.5, ↘",
            "135.0, ↘",
            "157.4, ↘",
            "157.5, ↓",
            "180.0, ↓",
            "-180.0, ↓",
            "-157.6, ↓",
            "-157.5, ↙",
            "-135.0, ↙",
            "-112.6, ↙",
            "-112.5, ←",
            "-90.0, ←",
            "-67.6, ←",
            "-67.5, ↖",
            "-45.0, ↖",
            "-22.6, ↖"
    })
    void directionalArrowThresholds(double bearing, String expectedArrow) {
        assertEquals(expectedArrow, RadarMath.direction(bearing));
    }

    @Test
    @DisplayName("Bearing calculation handles wrapped yaw values cleanly")
    void yawWrapping() {
        // Facing South with yaw=720 (equivalent to 0)
        double bearing1 = RadarMath.relativeBearing(0, 0, 720f, -10, 0);
        assertEquals(90.0, bearing1, DELTA);

        // Facing South with yaw=-360 (equivalent to 0)
        double bearing2 = RadarMath.relativeBearing(0, 0, -360f, -10, 0);
        assertEquals(90.0, bearing2, DELTA);

        // Offset positions (viewer at 100, 200, target at 100, 210 -> South)
        double bearing3 = RadarMath.relativeBearing(100, 200, 0f, 100, 210);
        assertEquals(0.0, bearing3, DELTA);
    }
}
