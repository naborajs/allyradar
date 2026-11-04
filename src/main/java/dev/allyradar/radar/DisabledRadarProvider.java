package dev.allyradar.radar;

import org.bukkit.entity.Player;

public final class DisabledRadarProvider implements RadarCompatibilityProvider {
    @Override
    public String implementationName() {
        return "Disabled radar";
    }

    @Override
    public void render(final Player viewer) {
    }

    @Override
    public void clear(final Player viewer) {
    }
}
