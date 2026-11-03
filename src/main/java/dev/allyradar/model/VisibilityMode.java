package dev.allyradar.model;

public enum VisibilityMode {
    PRIVATE,
    TEAM,
    PUBLIC;

    public static VisibilityMode parse(final String value) {
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Visibility must be PRIVATE, TEAM, or PUBLIC.");
        }
    }
}
