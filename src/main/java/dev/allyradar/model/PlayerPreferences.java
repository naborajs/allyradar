package dev.allyradar.model;

public record PlayerPreferences(
        VisibilityMode visibility,
        boolean radarEnabled,
        boolean hudEnabled,
        double customRadius
) {
    public PlayerPreferences(final VisibilityMode visibility, final boolean radarEnabled) {
        this(visibility, radarEnabled, true, 0.0);
    }
}

