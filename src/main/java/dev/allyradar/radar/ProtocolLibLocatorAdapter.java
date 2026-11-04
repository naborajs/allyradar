package dev.allyradar.radar;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import dev.allyradar.AllyRadar;
import dev.allyradar.model.PlayerPreferences;
import dev.allyradar.model.TeamSummary;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Intercepts Minecraft 1.21.6+ native Locator Bar tracked waypoint packets via ProtocolLib
 * to enforce team-only and privacy-controlled waypoint rendering on the Experience (XP) bar.
 */
public final class ProtocolLibLocatorAdapter implements LocatorBarFilter {
    private final AllyRadar plugin;
    private PacketAdapter packetListener;
    private volatile boolean active;

    public ProtocolLibLocatorAdapter(final AllyRadar plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean register() {
        try {
            final Plugin protocolLib = Bukkit.getPluginManager().getPlugin("ProtocolLib");
            if (protocolLib == null || !protocolLib.isEnabled()) {
                return false;
            }

            final ProtocolManager manager = ProtocolLibrary.getProtocolManager();
            this.packetListener = new PacketAdapter(plugin, PacketType.Play.Server.TRACKED_WAYPOINT) {
                @Override
                public void onPacketSending(final PacketEvent event) {
                    handlePacket(event);
                }
            };
            manager.addPacketListener(packetListener);
            this.active = true;
            plugin.getLogger().info("[AllyRadar] ProtocolLib detected: Native Locator Bar packet filter registered successfully.");
            return true;
        } catch (final Throwable t) {
            plugin.getLogger().log(Level.WARNING, "[AllyRadar] Failed to register ProtocolLib locator packet filter: " + t.getMessage(), t);
            this.active = false;
            return false;
        }
    }

    @Override
    public void unregister() {
        if (packetListener != null) {
            try {
                final ProtocolManager manager = ProtocolLibrary.getProtocolManager();
                if (manager != null) {
                    manager.removePacketListener(packetListener);
                }
            } catch (final Throwable ignored) {
            }
            packetListener = null;
        }
        this.active = false;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    private void handlePacket(final PacketEvent event) {
        if (!active || event == null || event.isCancelled()) {
            return;
        }
        final Player viewer = event.getPlayer();
        if (viewer == null || !viewer.isOnline()) {
            return;
        }

        try {
            final PacketContainer packet = event.getPacket();
            if (packet == null) {
                return;
            }
            final Object handle = packet.getHandle();
            if (handle == null) {
                return;
            }

            final UUID targetUuid = extractTargetUuid(handle);
            if (targetUuid == null) {
                return;
            }

            // A player's own waypoint is always authorized
            if (viewer.getUniqueId().equals(targetUuid)) {
                return;
            }

            final Player target = Bukkit.getPlayer(targetUuid);
            if (target == null || !target.isOnline()) {
                // If the target is offline or not a player, do not expose a ghost dot
                event.setCancelled(true);
                return;
            }

            // Cross-world tracking is prohibited
            if (!viewer.getWorld().equals(target.getWorld())) {
                event.setCancelled(true);
                return;
            }

            final PlayerPreferences viewerPrefs = plugin.preferences(viewer);
            final PlayerPreferences targetPrefs = plugin.preferences(target);
            final Optional<TeamSummary> viewerTeam = plugin.team(viewer);
            final Optional<TeamSummary> targetTeam = plugin.team(target);
            final boolean publicEnabled = plugin.getConfig().getBoolean("public-radar-enabled", true);

            final boolean canTrack = RadarPolicy.canTrack(
                    viewer.getUniqueId(),
                    targetUuid,
                    viewerPrefs,
                    targetPrefs,
                    viewerTeam,
                    targetTeam,
                    publicEnabled
            );

            if (!canTrack) {
                event.setCancelled(true);
                return;
            }

            if (viewerPrefs.customRadius() > 0.0) {
                final double distance = viewer.getLocation().distance(target.getLocation());
                if (distance > viewerPrefs.customRadius()) {
                    event.setCancelled(true);
                }
            }
        } catch (final Throwable t) {
            // Fail closed: if an authorization evaluation fails, cancel the waypoint packet
            event.setCancelled(true);
        }
    }

    private UUID extractTargetUuid(final Object handle) {
        if (handle == null) {
            return null;
        }
        try {
            Method waypointMethod = null;
            for (final Method m : handle.getClass().getMethods()) {
                if (m.getName().equals("waypoint") && m.getParameterCount() == 0) {
                    waypointMethod = m;
                    break;
                }
            }
            if (waypointMethod == null) {
                return null;
            }
            final Object waypoint = waypointMethod.invoke(handle);
            if (waypoint == null) {
                return null;
            }

            Method idMethod = null;
            for (final Method m : waypoint.getClass().getMethods()) {
                if (m.getName().equals("id") && m.getParameterCount() == 0) {
                    idMethod = m;
                    break;
                }
            }
            if (idMethod == null) {
                return null;
            }
            final Object idObj = idMethod.invoke(waypoint);
            if (idObj instanceof UUID uuid) {
                return uuid;
            }
            if (idObj != null) {
                try {
                    final Method leftMethod = idObj.getClass().getMethod("left");
                    final Object optional = leftMethod.invoke(idObj);
                    if (optional instanceof Optional<?> opt && opt.isPresent() && opt.get() instanceof UUID uuid) {
                        return uuid;
                    }
                } catch (final NoSuchMethodException ignored) {
                }
            }
        } catch (final Throwable ignored) {
        }
        return null;
    }
}
