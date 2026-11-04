package dev.allyradar.radar;

import dev.allyradar.model.PlayerPreferences;
import dev.allyradar.model.TeamSummary;
import dev.allyradar.model.VisibilityMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadarPipelineTest {
    private final UUID accountA = UUID.randomUUID(); // Viewer
    private final UUID accountB = UUID.randomUUID(); // Target
    private final UUID accountC = UUID.randomUUID(); // Third player
    private final TeamSummary sharedTeam = new TeamSummary(100L, "AlphaSquad", accountA, 2, true);
    private final TeamSummary otherTeam = new TeamSummary(200L, "BravoSquad", accountC, 1, true);
    private final TeamSummary teamRadarOff = new TeamSummary(100L, "AlphaSquad", accountA, 2, false);

    @Test
    @DisplayName("Scenario 1: Both accounts in same team, TEAM visibility, team radar on -> authorized")
    void sameTeamAuthorized() {
        final PlayerPreferences viewerPrefs = new PlayerPreferences(VisibilityMode.TEAM, true);
        final PlayerPreferences targetPrefs = new PlayerPreferences(VisibilityMode.TEAM, true);

        assertTrue(RadarPolicy.canTrack(accountA, accountB, viewerPrefs, targetPrefs,
                Optional.of(sharedTeam), Optional.of(sharedTeam), false));
    }

    @Test
    @DisplayName("Scenario 2: Target changes visibility to PRIVATE -> immediate complete denial")
    void targetPrivateDeniesAll() {
        final PlayerPreferences viewerPrefs = new PlayerPreferences(VisibilityMode.TEAM, true);
        final PlayerPreferences privatePrefs = new PlayerPreferences(VisibilityMode.PRIVATE, true);

        // Denies under same team
        assertFalse(RadarPolicy.canTrack(accountA, accountB, viewerPrefs, privatePrefs,
                Optional.of(sharedTeam), Optional.of(sharedTeam), true));
        // Denies even with public radar enabled on server
        assertFalse(RadarPolicy.canTrack(accountA, accountB, viewerPrefs, privatePrefs,
                Optional.empty(), Optional.empty(), true));
    }

    @Test
    @DisplayName("Scenario 3: Target selects TEAM but has no team -> effectively PRIVATE")
    void targetTeamWithoutTeamBehavesAsPrivate() {
        final PlayerPreferences viewerPrefs = new PlayerPreferences(VisibilityMode.TEAM, true);
        final PlayerPreferences targetPrefs = new PlayerPreferences(VisibilityMode.TEAM, true);

        // Viewer has team, target has NO team
        assertFalse(RadarPolicy.canTrack(accountA, accountB, viewerPrefs, targetPrefs,
                Optional.of(sharedTeam), Optional.empty(), true));
        // Neither has team
        assertFalse(RadarPolicy.canTrack(accountA, accountB, viewerPrefs, targetPrefs,
                Optional.empty(), Optional.empty(), true));
    }

    @Test
    @DisplayName("Scenario 4: Target PUBLIC requires server opt-in")
    void publicRadarRequiresServerOptIn() {
        final PlayerPreferences viewerPrefs = new PlayerPreferences(VisibilityMode.TEAM, true);
        final PlayerPreferences publicPrefs = new PlayerPreferences(VisibilityMode.PUBLIC, true);

        // Server public radar disabled -> false
        assertFalse(RadarPolicy.canTrack(accountA, accountB, viewerPrefs, publicPrefs,
                Optional.empty(), Optional.empty(), false));
        // Server public radar enabled -> true
        assertTrue(RadarPolicy.canTrack(accountA, accountB, viewerPrefs, publicPrefs,
                Optional.empty(), Optional.empty(), true));
    }

    @Test
    @DisplayName("Scenario 5: Team radar disabled denies all teammates")
    void teamRadarDisabledDeniesTracking() {
        final PlayerPreferences viewerPrefs = new PlayerPreferences(VisibilityMode.TEAM, true);
        final PlayerPreferences targetPrefs = new PlayerPreferences(VisibilityMode.TEAM, true);

        assertFalse(RadarPolicy.canTrack(accountA, accountB, viewerPrefs, targetPrefs,
                Optional.of(teamRadarOff), Optional.of(teamRadarOff), false));
    }

    @Test
    @DisplayName("Scenario 6: Viewer radar disabled denies receiving radar")
    void viewerRadarDisabledDeniesReceiving() {
        final PlayerPreferences viewerDisabled = new PlayerPreferences(VisibilityMode.TEAM, false);
        final PlayerPreferences targetPrefs = new PlayerPreferences(VisibilityMode.TEAM, true);

        assertFalse(RadarPolicy.canTrack(accountA, accountB, viewerDisabled, targetPrefs,
                Optional.of(sharedTeam), Optional.of(sharedTeam), true));
    }

    @Test
    @DisplayName("Scenario 7: Target leaves team or belongs to different team -> denies")
    void differentOrLeftTeamDenies() {
        final PlayerPreferences viewerPrefs = new PlayerPreferences(VisibilityMode.TEAM, true);
        final PlayerPreferences targetPrefs = new PlayerPreferences(VisibilityMode.TEAM, true);

        assertFalse(RadarPolicy.canTrack(accountA, accountB, viewerPrefs, targetPrefs,
                Optional.of(sharedTeam), Optional.of(otherTeam), false));
    }

    @Test
    @DisplayName("Scenario 8: Self tracking is forbidden under all configurations")
    void selfTrackingForbidden() {
        final PlayerPreferences publicPrefs = new PlayerPreferences(VisibilityMode.PUBLIC, true);
        assertFalse(RadarPolicy.canTrack(accountA, accountA, publicPrefs, publicPrefs,
                Optional.of(sharedTeam), Optional.of(sharedTeam), true));
    }

    @Test
    @DisplayName("Scenario 9: Distance and bearing pipeline calculation")
    void distanceAndBearingPipeline() {
        // Account A at (100, 100) facing North (yaw=180)
        // Account B at (100, 50): 50 meters directly North (ahead)
        final double distAhead = Math.hypot(100 - 100, 50 - 100);
        final double bearingAhead = RadarMath.relativeBearing(100, 100, 180f, 100, 50);
        assertEquals(50.0, distAhead, 0.001);
        assertEquals(0.0, bearingAhead, 0.001);
        assertEquals("↑", RadarMath.direction(bearingAhead));

        // Account B moves to (150, 100): 50 meters East (right for a North-facing viewer)
        final double distRight = Math.hypot(150 - 100, 100 - 100);
        final double bearingRight = RadarMath.relativeBearing(100, 100, 180f, 150, 100);
        assertEquals(50.0, distRight, 0.001);
        assertEquals(90.0, bearingRight, 0.001);
        assertEquals("→", RadarMath.direction(bearingRight));
    }
}
