package dev.allyradar.radar;

import dev.allyradar.model.PlayerPreferences;
import dev.allyradar.model.TeamSummary;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ActionBarRadarProvider implements RadarCompatibilityProvider {
    private final JavaPlugin plugin;
    private final AllyRadarAccess access;
    private final double maxDistance;
    private final boolean publicRadarEnabled;

    public ActionBarRadarProvider(final JavaPlugin plugin, final AllyRadarAccess access,
                                  final double maxDistance, final boolean publicRadarEnabled) {
        this.plugin = plugin;
        this.access = access;
        this.maxDistance = maxDistance;
        this.publicRadarEnabled = publicRadarEnabled;
    }

    @Override
    public String implementationName() {
        return "Filtered action-bar radar";
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
            final boolean sameTeam = viewerTeam.isPresent() && access.sameTeam(viewer, target);
            final boolean teamRadarEnabled = viewerTeam.map(TeamSummary::radarEnabled).orElse(false);
            if (!RadarPolicy.canTrack(viewer.getUniqueId(), target.getUniqueId(), viewerPreferences,
                    targetPreferences, sameTeam, teamRadarEnabled, publicRadarEnabled)) {
                continue;
            }
            final double distance = viewer.getLocation().distance(target.getLocation());
            if (distance > maxDistance) {
                continue;
            }
            visible.add(new TrackedResult(target.getName(), distance,
                    relativeBearing(viewer.getLocation(), target.getLocation())));
        }
        visible.sort(Comparator.comparingDouble(TrackedResult::distance));
        if (visible.isEmpty()) {
            viewer.sendActionBar(Component.text("AllyRadar: no authorized teammates nearby", NamedTextColor.GRAY));
            return;
        }
        final StringBuilder message = new StringBuilder("Radar ");
        for (int index = 0; index < Math.min(visible.size(), 4); index++) {
            if (index > 0) {
                message.append("  ");
            }
            final TrackedResult result = visible.get(index);
            message.append(result.name()).append(' ').append(direction(result.bearing()))
                    .append(' ').append(Math.round(result.distance())).append('m');
        }
        if (visible.size() > 4) {
            message.append(" +").append(visible.size() - 4);
        }
        viewer.sendActionBar(Component.text(message.toString(), NamedTextColor.AQUA));
    }

    @Override
    public void clear(final Player viewer) {
        viewer.sendActionBar(Component.empty());
    }

    private double relativeBearing(final Location viewer, final Location target) {
        final double absolute = Math.toDegrees(Math.atan2(target.getX() - viewer.getX(), target.getZ() - viewer.getZ()));
        return ((absolute - viewer.getYaw() + 540.0) % 360.0) - 180.0;
    }

    private String direction(final double bearing) {
        if (bearing >= -22.5 && bearing < 22.5) return "↑";
        if (bearing >= 22.5 && bearing < 67.5) return "↗";
        if (bearing >= 67.5 && bearing < 112.5) return "→";
        if (bearing >= 112.5 && bearing < 157.5) return "↘";
        if (bearing >= 157.5 || bearing < -157.5) return "↓";
        if (bearing >= -157.5 && bearing < -112.5) return "↙";
        if (bearing >= -112.5 && bearing < -67.5) return "←";
        return "↖";
    }

    private record TrackedResult(String name, double distance, double bearing) {
    }

    public interface AllyRadarAccess {
        PlayerPreferences preferences(Player player);

        Optional<TeamSummary> team(Player player);

        boolean sameTeam(Player first, Player second);
    }
}
