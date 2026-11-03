package dev.allyradar.radar;

import org.bukkit.entity.Player;

public interface RadarCompatibilityProvider {
    String implementationName();

    void render(Player viewer);

    void clear(Player viewer);
}
