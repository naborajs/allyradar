package dev.allyradar.radar;

import dev.allyradar.model.PlayerPreferences;
import dev.allyradar.model.TeamSummary;
import dev.allyradar.model.VisibilityMode;

import java.util.Optional;
import java.util.UUID;

public final class RadarPolicy {
    private RadarPolicy() {
    }

    public static VisibilityMode effectiveVisibility(final PlayerPreferences preferences,
                                                     final Optional<TeamSummary> team) {
        if (preferences == null || preferences.visibility() == null) {
            return VisibilityMode.PRIVATE;
        }
        return switch (preferences.visibility()) {
            case PRIVATE -> VisibilityMode.PRIVATE;
            case TEAM, PUBLIC -> (team != null && team.isPresent()) ? VisibilityMode.TEAM : (preferences.visibility() == VisibilityMode.PUBLIC ? VisibilityMode.PUBLIC : VisibilityMode.PRIVATE);
        };
    }

    public static boolean canTrack(final UUID viewer, final UUID target, final PlayerPreferences viewerPreferences,
                                   final PlayerPreferences targetPreferences, final Optional<TeamSummary> viewerTeam,
                                   final Optional<TeamSummary> targetTeam, final boolean publicRadarEnabled) {
        if (viewer == null || target == null || viewer.equals(target)) {
            return false;
        }
        if (viewerPreferences == null || !viewerPreferences.radarEnabled()) {
            return false;
        }
        if (targetPreferences == null) {
            return false;
        }

        // 1. If target belongs to a team, STRICT team isolation applies:
        // ONLY teammates in the exact same team can EVER track them!
        // No player outside the team (solo or another team) can EVER track a teamed player!
        if (targetTeam != null && targetTeam.isPresent()) {
            if (viewerTeam == null || viewerTeam.isEmpty()) {
                return false; // Viewer is outside the team (solo) -> strictly DENIED
            }
            if (viewerTeam.get().id() != targetTeam.get().id()) {
                return false; // Viewer is on another team (enemy) -> strictly DENIED
            }
            if (!viewerTeam.get().radarEnabled() || !targetTeam.get().radarEnabled()) {
                return false; // Team radar is turned off -> DENIED
            }
            // Teammates can track each other unless the target explicitly hid with PRIVATE
            return targetPreferences.visibility() != VisibilityMode.PRIVATE;
        }

        // 2. Target is SOLO (no team):
        final VisibilityMode effective = effectiveVisibility(targetPreferences, targetTeam);
        return switch (effective) {
            case PRIVATE, TEAM -> false; // Solo player is hidden unless they opted into PUBLIC
            case PUBLIC -> publicRadarEnabled; // Solo player chose PUBLIC -> visible if public radar enabled
        };
    }

    public static boolean canTrack(final UUID viewer, final UUID target, final PlayerPreferences viewerPreferences,
                                   final PlayerPreferences targetPreferences, final boolean sameTeam,
                                   final boolean teamRadarEnabled, final boolean publicRadarEnabled) {
        if (viewer == null || target == null || viewer.equals(target)
                || viewerPreferences == null || !viewerPreferences.radarEnabled()
                || targetPreferences == null || targetPreferences.visibility() == null) {
            return false;
        }
        return switch (targetPreferences.visibility()) {
            case PRIVATE -> false;
            case TEAM -> sameTeam && teamRadarEnabled;
            case PUBLIC -> sameTeam ? teamRadarEnabled : publicRadarEnabled;
        };
    }
}

