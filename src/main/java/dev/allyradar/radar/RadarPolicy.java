package dev.allyradar.radar;

import dev.allyradar.model.PlayerPreferences;
import dev.allyradar.model.VisibilityMode;

import java.util.UUID;

public final class RadarPolicy {
    private RadarPolicy() {
    }

    public static boolean canTrack(final UUID viewer, final UUID target, final PlayerPreferences viewerPreferences,
                                   final PlayerPreferences targetPreferences, final boolean sameTeam,
                                   final boolean teamRadarEnabled, final boolean publicRadarEnabled) {
        if (viewer.equals(target) || !viewerPreferences.radarEnabled()) {
            return false;
        }
        return switch (targetPreferences.visibility()) {
            case PRIVATE -> false;
            case TEAM -> sameTeam && teamRadarEnabled;
            case PUBLIC -> publicRadarEnabled;
        };
    }
}
