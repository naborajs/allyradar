package dev.allyradar.radar;

import dev.allyradar.model.PlayerPreferences;
import dev.allyradar.model.TeamSummary;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ActionBarRadarProvider implements RadarCompatibilityProvider {
    private final JavaPlugin plugin;
    private final AllyRadarAccess access;
    private final double maxDistance;
    private final boolean publicRadarEnabled;
    private final Set<UUID> activeHudViewers = Collections.newSetFromMap(new ConcurrentHashMap<>());

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
        if (!viewerPreferences.radarEnabled() || !viewerPreferences.hudEnabled()) {
            if (activeHudViewers.remove(viewer.getUniqueId())) {
                viewer.sendActionBar(Component.text(" "));
            }
            return;
        }
        final double effectiveMaxDistance = viewerPreferences.customRadius() > 0.0
                ? Math.min(viewerPreferences.customRadius(), maxDistance)
                : maxDistance;
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
            if (distance > effectiveMaxDistance) {
                continue;
            }
            visible.add(new TrackedResult(target.getName(), distance,
                    relativeBearing(viewer.getLocation(), target.getLocation())));
        }
        visible.sort(Comparator.comparingDouble(TrackedResult::distance));
        if (visible.isEmpty()) {
            if (activeHudViewers.remove(viewer.getUniqueId())) {
                viewer.sendActionBar(Component.text(" "));
            }
            return;
        }
        activeHudViewers.add(viewer.getUniqueId());
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
        activeHudViewers.remove(viewer.getUniqueId());
        viewer.sendActionBar(Component.text(" "));
    }

    public static double relativeBearing(final Location viewer, final Location target) {
        return RadarMath.relativeBearing(viewer.getX(), viewer.getZ(), viewer.getYaw(), target.getX(), target.getZ());
    }

    public static double relativeBearing(final double viewerX, final double viewerZ, final float viewerYaw,
                                         final double targetX, final double targetZ) {
        return RadarMath.relativeBearing(viewerX, viewerZ, viewerYaw, targetX, targetZ);
    }

    public static String direction(final double bearing) {
        return RadarMath.direction(bearing);
    }

    private record TrackedResult(String name, double distance, double bearing) {
    }

    public interface AllyRadarAccess {
        PlayerPreferences preferences(Player player);

        Optional<TeamSummary> team(Player player);

        boolean sameTeam(Player first, Player second);
    }
}
