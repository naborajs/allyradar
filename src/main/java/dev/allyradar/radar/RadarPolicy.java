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
            case TEAM -> (team != null && team.isPresent()) ? VisibilityMode.TEAM : VisibilityMode.PRIVATE;
            case PUBLIC -> VisibilityMode.PUBLIC;
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

        final VisibilityMode effective = effectiveVisibility(targetPreferences, targetTeam);
        return switch (effective) {
            case PRIVATE -> false;
            case TEAM -> {
                // Target has TEAM visibility: viewer MUST be in the exact same team!
                if (viewerTeam == null || viewerTeam.isEmpty() || targetTeam == null || targetTeam.isEmpty()) {
                    yield false; // Outside solo player -> strictly DENIED
                }
                if (viewerTeam.get().id() != targetTeam.get().id()) {
                    yield false; // Different team (enemy) -> strictly DENIED
                }
                if (!viewerTeam.get().radarEnabled() || !targetTeam.get().radarEnabled()) {
                    yield false; // Team radar disabled -> DENIED
                }
                yield true; // Same team with radar enabled -> ALLOWED
            }
            case PUBLIC -> publicRadarEnabled; // Explicit PUBLIC visibility -> visible to everyone with radar ON
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
            case PUBLIC -> publicRadarEnabled;
        };
    }
}

