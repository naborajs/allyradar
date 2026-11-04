package dev.allyradar.radar;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class CompositeRadarProvider implements RadarCompatibilityProvider {
    private final List<RadarCompatibilityProvider> delegates;

    public CompositeRadarProvider(final List<RadarCompatibilityProvider> delegates) {
        this.delegates = List.copyOf(Objects.requireNonNull(delegates, "delegates"));
    }

    @Override
    public String implementationName() {
        return delegates.stream()
                .map(RadarCompatibilityProvider::implementationName)
                .collect(Collectors.joining(" + ", "Composite [", "]"));
    }

    @Override
    public void render(final Player viewer) {
        for (final RadarCompatibilityProvider delegate : delegates) {
            delegate.render(viewer);
        }
    }

    @Override
    public void clear(final Player viewer) {
        for (final RadarCompatibilityProvider delegate : delegates) {
            delegate.clear(viewer);
        }
    }

    public List<RadarCompatibilityProvider> delegates() {
        return delegates;
    }
}
