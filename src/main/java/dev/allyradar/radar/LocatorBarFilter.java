package dev.allyradar.radar;

/**
 * Pluggable filter interface for Minecraft's native Locator Bar (tracked waypoints on the XP bar).
 */
public interface LocatorBarFilter {
    /**
     * Registers the packet filter with the underlying network pipeline.
     *
     * @return true if registration succeeded and filtering is active, false otherwise.
     */
    boolean register();

    /**
     * Unregisters the packet filter and restores default pipeline handling.
     */
    void unregister();

    /**
     * Returns whether the filter is actively intercepting and filtering waypoint packets.
     */
    boolean isActive();
}
