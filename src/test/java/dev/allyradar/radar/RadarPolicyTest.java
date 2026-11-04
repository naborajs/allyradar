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

class RadarPolicyTest {
    private final UUID viewer = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();
    private final PlayerPreferences receiving = new PlayerPreferences(VisibilityMode.TEAM, true);
    private final TeamSummary teamAlpha = new TeamSummary(1L, "Team Alpha", UUID.randomUUID(), 2, true);
    private final TeamSummary teamBeta = new TeamSummary(2L, "Team Beta", UUID.randomUUID(), 2, true);
    private final TeamSummary teamAlphaDisabled = new TeamSummary(1L, "Team Alpha", UUID.randomUUID(), 2, false);

    @Test
    void privateAlwaysHidesTarget() {
        assertFalse(can(VisibilityMode.PRIVATE, true, true, true));
        assertFalse(RadarPolicy.canTrack(viewer, target, receiving,
                new PlayerPreferences(VisibilityMode.PRIVATE, true),
                Optional.of(teamAlpha), Optional.of(teamAlpha), true));
    }

    @Test
    void teamVisibilityRequiresBothMembershipAndTeamRadar() {
        assertTrue(can(VisibilityMode.TEAM, true, true, false));
        assertFalse(can(VisibilityMode.TEAM, false, true, false));
        assertFalse(can(VisibilityMode.TEAM, true, false, false));
    }

    @Test
    void publicVisibilityRequiresServerOptIn() {
        assertTrue(can(VisibilityMode.PUBLIC, false, false, true));
        assertFalse(can(VisibilityMode.PUBLIC, false, false, false));
    }

    @Test
    void receiverCanDisableAllRadar() {
        assertFalse(RadarPolicy.canTrack(viewer, target, new PlayerPreferences(VisibilityMode.TEAM, false),
                new PlayerPreferences(VisibilityMode.PUBLIC, true), false, false, true));
        assertFalse(RadarPolicy.canTrack(viewer, target, new PlayerPreferences(VisibilityMode.TEAM, false),
                new PlayerPreferences(VisibilityMode.PUBLIC, true), Optional.of(teamAlpha), Optional.of(teamAlpha), true));
    }

    @Test
    void teamVisibilityWithoutTeamBehavesAsPrivate() {
        final PlayerPreferences targetPrefs = new PlayerPreferences(VisibilityMode.TEAM, true);

        // Effective visibility must be PRIVATE
        assertEquals(VisibilityMode.PRIVATE, RadarPolicy.effectiveVisibility(targetPrefs, Optional.empty()));
        assertEquals(VisibilityMode.PRIVATE, RadarPolicy.effectiveVisibility(targetPrefs, null));

        // canTrack must return false even if viewer is in a team and public radar is enabled
        assertFalse(RadarPolicy.canTrack(viewer, target, receiving, targetPrefs,
                Optional.of(teamAlpha), Optional.empty(), true));
        assertFalse(RadarPolicy.canTrack(viewer, target, receiving, targetPrefs,
                Optional.of(teamAlpha), Optional.empty(), false));
    }

    @Test
    void effectiveVisibilityResolvesCorrectly() {
        // Null preferences
        assertEquals(VisibilityMode.PRIVATE, RadarPolicy.effectiveVisibility(null, Optional.of(teamAlpha)));
        assertEquals(VisibilityMode.PRIVATE, RadarPolicy.effectiveVisibility(null, Optional.empty()));

        // Null visibility within preferences
        assertEquals(VisibilityMode.PRIVATE, RadarPolicy.effectiveVisibility(new PlayerPreferences(null, true), Optional.of(teamAlpha)));

        // PRIVATE visibility
        assertEquals(VisibilityMode.PRIVATE, RadarPolicy.effectiveVisibility(
                new PlayerPreferences(VisibilityMode.PRIVATE, true), Optional.of(teamAlpha)));
        assertEquals(VisibilityMode.PRIVATE, RadarPolicy.effectiveVisibility(
                new PlayerPreferences(VisibilityMode.PRIVATE, true), Optional.empty()));

        // TEAM visibility without team
        assertEquals(VisibilityMode.PRIVATE, RadarPolicy.effectiveVisibility(
                new PlayerPreferences(VisibilityMode.TEAM, true), Optional.empty()));
        assertEquals(VisibilityMode.PRIVATE, RadarPolicy.effectiveVisibility(
                new PlayerPreferences(VisibilityMode.TEAM, true), null));

        // TEAM visibility with team
        assertEquals(VisibilityMode.TEAM, RadarPolicy.effectiveVisibility(
                new PlayerPreferences(VisibilityMode.TEAM, true), Optional.of(teamAlpha)));

        // PUBLIC visibility without team is PUBLIC; with team evaluates to TEAM (confined to team)
        assertEquals(VisibilityMode.PUBLIC, RadarPolicy.effectiveVisibility(
                new PlayerPreferences(VisibilityMode.PUBLIC, true), Optional.empty()));
        assertEquals(VisibilityMode.TEAM, RadarPolicy.effectiveVisibility(
                new PlayerPreferences(VisibilityMode.PUBLIC, true), Optional.of(teamAlpha)));
    }

    @Test
    void outsidePlayerCannotTrackTeamedPlayerEvenIfPublic() {
        final PlayerPreferences publicPrefs = new PlayerPreferences(VisibilityMode.PUBLIC, true);
        // Viewer is solo, target is in teamAlpha with PUBLIC preference -> strictly DENIED
        assertFalse(RadarPolicy.canTrack(viewer, target, receiving, publicPrefs,
                Optional.empty(), Optional.of(teamAlpha), true));
        // Viewer is in teamBeta, target is in teamAlpha with PUBLIC preference -> strictly DENIED
        assertFalse(RadarPolicy.canTrack(viewer, target, receiving, publicPrefs,
                Optional.of(teamBeta), Optional.of(teamAlpha), true));
        // Teammate in same teamAlpha CAN track them!
        assertTrue(RadarPolicy.canTrack(viewer, target, receiving, publicPrefs,
                Optional.of(teamAlpha), Optional.of(teamAlpha), true));
    }

    @Test
    void nullInputsFailClosed() {
        final PlayerPreferences targetPrefs = new PlayerPreferences(VisibilityMode.PUBLIC, true);

        // Centralized canTrack with nulls
        assertFalse(RadarPolicy.canTrack(null, target, receiving, targetPrefs, Optional.of(teamAlpha), Optional.of(teamAlpha), true));
        assertFalse(RadarPolicy.canTrack(viewer, null, receiving, targetPrefs, Optional.of(teamAlpha), Optional.of(teamAlpha), true));
        assertFalse(RadarPolicy.canTrack(viewer, target, null, targetPrefs, Optional.of(teamAlpha), Optional.of(teamAlpha), true));
        assertFalse(RadarPolicy.canTrack(viewer, target, receiving, null, Optional.of(teamAlpha), Optional.of(teamAlpha), true));
        assertFalse(RadarPolicy.canTrack(viewer, target, receiving, new PlayerPreferences(VisibilityMode.TEAM, true), null, Optional.of(teamAlpha), true));
        assertFalse(RadarPolicy.canTrack(viewer, target, receiving, new PlayerPreferences(VisibilityMode.TEAM, true), Optional.of(teamAlpha), null, true));

        // Legacy canTrack with nulls
        assertFalse(RadarPolicy.canTrack(null, target, receiving, targetPrefs, true, true, true));
        assertFalse(RadarPolicy.canTrack(viewer, null, receiving, targetPrefs, true, true, true));
        assertFalse(RadarPolicy.canTrack(viewer, target, null, targetPrefs, true, true, true));
        assertFalse(RadarPolicy.canTrack(viewer, target, receiving, null, true, true, true));
        assertFalse(RadarPolicy.canTrack(viewer, target, receiving, new PlayerPreferences(null, true), true, true, true));
    }

    @Test
    void differentTeamsWithTeamVisibilityCannotTrack() {
        final PlayerPreferences targetPrefs = new PlayerPreferences(VisibilityMode.TEAM, true);
        assertFalse(RadarPolicy.canTrack(viewer, target, receiving, targetPrefs,
                Optional.of(teamAlpha), Optional.of(teamBeta), false));
        assertFalse(RadarPolicy.canTrack(viewer, target, receiving, targetPrefs,
                Optional.of(teamAlpha), Optional.of(teamBeta), true));
    }

    @Test
    void teamRadarDisabledCannotTrack() {
        final PlayerPreferences targetPrefs = new PlayerPreferences(VisibilityMode.TEAM, true);
        assertFalse(RadarPolicy.canTrack(viewer, target, receiving, targetPrefs,
                Optional.of(teamAlphaDisabled), Optional.of(teamAlphaDisabled), false));
        assertFalse(RadarPolicy.canTrack(viewer, target, receiving, targetPrefs,
                Optional.of(teamAlphaDisabled), Optional.of(teamAlphaDisabled), true));
        // Asymmetric: viewer's team disabled, target's team enabled
        assertFalse(RadarPolicy.canTrack(viewer, target, receiving, targetPrefs,
                Optional.of(teamAlphaDisabled), Optional.of(teamAlpha), true));
        // Asymmetric: viewer's team enabled, target's team disabled
        assertFalse(RadarPolicy.canTrack(viewer, target, receiving, targetPrefs,
                Optional.of(teamAlpha), Optional.of(teamAlphaDisabled), true));
    }

    @Test
    void selfTrackingIsForbidden() {
        final PlayerPreferences publicPrefs = new PlayerPreferences(VisibilityMode.PUBLIC, true);
        final PlayerPreferences teamPrefs = new PlayerPreferences(VisibilityMode.TEAM, true);

        // Self tracking must be denied on both overloads regardless of visibility
        assertFalse(RadarPolicy.canTrack(viewer, viewer, receiving, publicPrefs,
                Optional.of(teamAlpha), Optional.of(teamAlpha), true));
        assertFalse(RadarPolicy.canTrack(viewer, viewer, receiving, teamPrefs,
                Optional.of(teamAlpha), Optional.of(teamAlpha), true));
        assertFalse(RadarPolicy.canTrack(viewer, viewer, receiving, publicPrefs, true, true, true));
        assertFalse(RadarPolicy.canTrack(viewer, viewer, receiving, teamPrefs, true, true, true));
    }

    @Test
    void sameTeamWithRadarEnabledCanTrack() {
        final PlayerPreferences targetPrefs = new PlayerPreferences(VisibilityMode.TEAM, true);
        assertTrue(RadarPolicy.canTrack(viewer, target, receiving, targetPrefs,
                Optional.of(teamAlpha), Optional.of(teamAlpha), false));
    }

    @Test
    void publicRadarWithServerEnabledCanTrack() {
        final PlayerPreferences targetPrefs = new PlayerPreferences(VisibilityMode.PUBLIC, true);
        assertTrue(RadarPolicy.canTrack(viewer, target, receiving, targetPrefs,
                Optional.empty(), Optional.empty(), true));
        assertFalse(RadarPolicy.canTrack(viewer, target, receiving, targetPrefs,
                Optional.empty(), Optional.empty(), false));
    }

    private boolean can(final VisibilityMode targetVisibility, final boolean sameTeam,
                        final boolean teamRadar, final boolean publicRadar) {
        return RadarPolicy.canTrack(viewer, target, receiving,
                new PlayerPreferences(targetVisibility, true), sameTeam, teamRadar, publicRadar);
    }
}

