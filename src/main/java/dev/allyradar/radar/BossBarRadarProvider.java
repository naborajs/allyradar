package dev.allyradar.radar;

import dev.allyradar.model.PlayerPreferences;
import dev.allyradar.model.TeamSummary;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BossBarRadarProvider implements RadarCompatibilityProvider {
    private final JavaPlugin plugin;
    private final ActionBarRadarProvider.AllyRadarAccess access;
    private final double maxDistance;
    private final boolean publicRadarEnabled;
    private final Map<UUID, BossBar> activeBars = new ConcurrentHashMap<>();

    public BossBarRadarProvider(final JavaPlugin plugin, final ActionBarRadarProvider.AllyRadarAccess access,
                                final double maxDistance, final boolean publicRadarEnabled) {
        this.plugin = plugin;
        this.access = access;
        this.maxDistance = maxDistance;
        this.publicRadarEnabled = publicRadarEnabled;
    }

    @Override
    public String implementationName() {
        return "Filtered boss-bar radar";
    }

    @Override
    public void render(final Player viewer) {
        final PlayerPreferences viewerPreferences = access.preferences(viewer);
        if (!viewerPreferences.radarEnabled()) {
            clear(viewer);
            return;
        }
        final Optional<TeamSummary> viewerTeam = access.team(viewer);
        final List<TrackedResult> visible = new ArrayList<>();
        for (final Player target : plugin.getServer().getOnlinePlayers()) {
            if (target.equals(viewer) || !target.getWorld().equals(viewer.getWorld())) {
                continue;
            }
            final PlayerPreferences targetPreferences = access.preferences(target);
            final Optional<TeamSummary> targetTeam = access.team(target);
            if (!RadarPolicy.canTrack(viewer.getUniqueId(), target.getUniqueId(), viewerPreferences,
                    targetPreferences, viewerTeam, targetTeam, publicRadarEnabled)) {
                continue;
            }
            final double distance = viewer.getLocation().distance(target.getLocation());
            if (distance > maxDistance) {
                continue;
            }
            visible.add(new TrackedResult(target.getName(), distance,
                    RadarMath.relativeBearing(viewer.getLocation().getX(), viewer.getLocation().getZ(),
                            viewer.getLocation().getYaw(), target.getLocation().getX(), target.getLocation().getZ())));
        }
        visible.sort(Comparator.comparingDouble(TrackedResult::distance));
        if (visible.isEmpty()) {
            clear(viewer);
            return;
        }

        final StringBuilder message = new StringBuilder("Radar ");
        for (int index = 0; index < Math.min(visible.size(), 4); index++) {
            if (index > 0) {
                message.append("  ");
            }
            final TrackedResult result = visible.get(index);
            message.append(result.name()).append(' ').append(RadarMath.direction(result.bearing()))
                    .append(' ').append(Math.round(result.distance())).append('m');
        }
        if (visible.size() > 4) {
            message.append(" +").append(visible.size() - 4);
        }

        final Component title = Component.text(message.toString(), NamedTextColor.AQUA);
        final BossBar bar = activeBars.get(viewer.getUniqueId());
        if (bar != null) {
            bar.name(title);
        } else {
            final BossBar newBar = BossBar.bossBar(title, 1.0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
            activeBars.put(viewer.getUniqueId(), newBar);
            viewer.showBossBar(newBar);
        }
    }

    @Override
    public void clear(final Player viewer) {
        final BossBar bar = activeBars.remove(viewer.getUniqueId());
        if (bar != null) {
            viewer.hideBossBar(bar);
        }
    }

    private record TrackedResult(String name, double distance, double bearing) {
    }
}
