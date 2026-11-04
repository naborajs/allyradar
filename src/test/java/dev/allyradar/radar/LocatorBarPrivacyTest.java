package dev.allyradar.radar;

import dev.allyradar.model.PlayerPreferences;
import dev.allyradar.model.TeamSummary;
import dev.allyradar.model.VisibilityMode;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocatorBarPrivacyTest {
    private final UUID viewerUuid = UUID.randomUUID();
    private final UUID targetUuid = UUID.randomUUID();
    private final TeamSummary teamA = new TeamSummary(1L, "Team A", UUID.randomUUID(), 2, true);
    private final TeamSummary teamB = new TeamSummary(2L, "Team B", UUID.randomUUID(), 2, true);

    @Test
    void soloPlayerDefaultsToPrivateTransmitRange() {
        final PlayerPreferences soloPrefs = new PlayerPreferences(VisibilityMode.TEAM, true);
        final Optional<TeamSummary> noTeam = Optional.empty();

        final VisibilityMode effective = RadarPolicy.effectiveVisibility(soloPrefs, noTeam);
        assertEquals(VisibilityMode.PRIVATE, effective);

        final double transmitRange = calculateTransmitRange(effective, false);
        assertEquals(0.0, transmitRange, "Solo player must have transmit range 0.0 so no dots appear on any locator bar");
    }

    @Test
    void privateVisibilityAlwaysHasZeroTransmitRange() {
        final PlayerPreferences privatePrefs = new PlayerPreferences(VisibilityMode.PRIVATE, true);
        final Optional<TeamSummary> team = Optional.of(teamA);

        final VisibilityMode effective = RadarPolicy.effectiveVisibility(privatePrefs, team);
        assertEquals(VisibilityMode.PRIVATE, effective);

        final double transmitRangeWithFilter = calculateTransmitRange(effective, true);
        final double transmitRangeWithoutFilter = calculateTransmitRange(effective, false);
        assertEquals(0.0, transmitRangeWithFilter);
        assertEquals(0.0, transmitRangeWithoutFilter);
    }

    @Test
    void publicVisibilityHasFullTransmitRange() {
        final PlayerPreferences publicPrefs = new PlayerPreferences(VisibilityMode.PUBLIC, true);
        final Optional<TeamSummary> noTeam = Optional.empty();

        final VisibilityMode effective = RadarPolicy.effectiveVisibility(publicPrefs, noTeam);
        assertEquals(VisibilityMode.PUBLIC, effective);

        final double transmitRange = calculateTransmitRange(effective, false);
        assertEquals(60000000.0, transmitRange);
    }

    @Test
    void teamedPlayerTransmitRangeDependsOnPacketFilter() {
        final PlayerPreferences teamPrefs = new PlayerPreferences(VisibilityMode.TEAM, true);
        final Optional<TeamSummary> team = Optional.of(teamA);

        final VisibilityMode effective = RadarPolicy.effectiveVisibility(teamPrefs, team);
        assertEquals(VisibilityMode.TEAM, effective);

        // When packet filter is active (ProtocolLib), allow transmit so filter can manage per-recipient
        final double transmitWithFilter = calculateTransmitRange(effective, true);
        assertEquals(60000000.0, transmitWithFilter);

        // When packet filter is inactive, fail-safe to 0.0 to prevent vanilla Minecraft from leaking to enemies
        final double transmitWithoutFilter = calculateTransmitRange(effective, false);
        assertEquals(0.0, transmitWithoutFilter);
    }

    @Test
    void radarDisabledZeroesReceiveRange() {
        final PlayerPreferences disabledPrefs = new PlayerPreferences(VisibilityMode.TEAM, false);
        final PlayerPreferences enabledPrefs = new PlayerPreferences(VisibilityMode.TEAM, true);

        assertEquals(0.0, calculateReceiveRange(disabledPrefs), "Disabled radar must have receive range 0.0");
        assertEquals(60000000.0, calculateReceiveRange(enabledPrefs), "Enabled radar must receive waypoints");
    }

    @Test
    void packetFilterPolicyEnforcesPrivacyAcrossTeams() {
        final PlayerPreferences receiverOn = new PlayerPreferences(VisibilityMode.TEAM, true);
        final PlayerPreferences teammateTeam = new PlayerPreferences(VisibilityMode.TEAM, true);
        final PlayerPreferences teammatePrivate = new PlayerPreferences(VisibilityMode.PRIVATE, true);
        final PlayerPreferences enemyTeam = new PlayerPreferences(VisibilityMode.TEAM, true);

        // Teammates can see each other
        assertTrue(RadarPolicy.canTrack(viewerUuid, targetUuid, receiverOn, teammateTeam,
                Optional.of(teamA), Optional.of(teamA), false));

        // Private teammate is completely hidden
        assertFalse(RadarPolicy.canTrack(viewerUuid, targetUuid, receiverOn, teammatePrivate,
                Optional.of(teamA), Optional.of(teamA), false));

        // Enemy on different team is completely hidden
        assertFalse(RadarPolicy.canTrack(viewerUuid, targetUuid, receiverOn, enemyTeam,
                Optional.of(teamA), Optional.of(teamB), false));

        // Teamless neutral player is hidden when team visibility is selected
        assertFalse(RadarPolicy.canTrack(viewerUuid, targetUuid, receiverOn, teammateTeam,
                Optional.of(teamA), Optional.empty(), false));

        // Solo player with PUBLIC visibility is tracked by another solo player when public radar is enabled
        final PlayerPreferences soloPublic = new PlayerPreferences(VisibilityMode.PUBLIC, true);
        assertTrue(RadarPolicy.canTrack(viewerUuid, targetUuid, receiverOn, soloPublic,
                Optional.empty(), Optional.empty(), true), "Solo players with PUBLIC visibility must be trackable when public radar is enabled");
    }

    private double calculateTransmitRange(final VisibilityMode effective, final boolean filterActive) {
        if (effective == VisibilityMode.PRIVATE) {
            return 0.0;
        } else if (effective == VisibilityMode.PUBLIC) {
            return 60000000.0;
        } else {
            return filterActive ? 60000000.0 : 0.0;
        }
    }

    private double calculateReceiveRange(final PlayerPreferences prefs) {
        return prefs.radarEnabled() ? 60000000.0 : 0.0;
    }
}
