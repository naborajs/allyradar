package dev.allyradar;

import dev.allyradar.command.AllyRadarCommand;
import dev.allyradar.model.PlayerPreferences;
import dev.allyradar.model.TeamSummary;
import dev.allyradar.model.VisibilityMode;
import dev.allyradar.radar.ActionBarRadarProvider;
import dev.allyradar.radar.BossBarRadarProvider;
import dev.allyradar.radar.CompositeRadarProvider;
import dev.allyradar.radar.DisabledRadarProvider;
import dev.allyradar.radar.LocatorBarFilter;
import dev.allyradar.radar.RadarCompatibilityProvider;
import dev.allyradar.radar.RadarPolicy;
import dev.allyradar.storage.AllyRepository;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AllyRadar extends JavaPlugin implements Listener, ActionBarRadarProvider.AllyRadarAccess {
    private AllyRepository repository;
    private RadarCompatibilityProvider radarProvider;
    private LocatorBarFilter locatorBarFilter;
    private BukkitTask radarTask;
    private int maximumTeamSize;
    private long invitationExpirationHours;
    private long radarUpdateIntervalTicks;
    private double radarMaxDistance;
    private final Map<UUID, PlayerPreferences> preferenceCache = new ConcurrentHashMap<>();
    private final Map<UUID, Optional<TeamSummary>> teamCache = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        maximumTeamSize = Math.max(2, getConfig().getInt("max-team-size", 10));
        invitationExpirationHours = Math.max(1L, getConfig().getLong("invitation-expiration-hours", 48L));
        radarUpdateIntervalTicks = Math.max(1L, getConfig().getLong("radar-update-interval-ticks", 20L));
        radarMaxDistance = Math.max(1.0, getConfig().getDouble("radar-max-distance", 1000.0));
        final VisibilityMode defaultVisibility = visibilityFromConfig();
        repository = new AllyRepository(this, getConfig().getString("database-file", "allyradar.db"),
                defaultVisibility, getConfig().getBoolean("default-radar-enabled", true),
                getConfig().getBoolean("default-team-radar-enabled", true));
        radarProvider = createRadarProvider(getConfig().getString("radar-display-mode", "ACTION_BAR"));
        final AllyRadarCommand command = new AllyRadarCommand(this);
        registerCommand("ally", command);
        registerCommand("team", command);
        registerCommand("radar", command);
        registerCommand("a", command);
        getServer().getPluginManager().registerEvents(this, this);
        radarTask = getServer().getScheduler().runTaskTimer(this, () ->
                getServer().getOnlinePlayers().forEach(radarProvider::render),
                radarUpdateIntervalTicks, radarUpdateIntervalTicks);

        if (getConfig().getBoolean("locator-bar-filter-enabled", true)
                && getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            try {
                final Class<?> clazz = Class.forName("dev.allyradar.radar.ProtocolLibLocatorAdapter");
                this.locatorBarFilter = (LocatorBarFilter) clazz.getConstructor(AllyRadar.class).newInstance(this);
                this.locatorBarFilter.register();
            } catch (final Throwable t) {
                getLogger().warning("Could not initialize ProtocolLib locator adapter: " + t.getMessage());
            }
        }
        getLogger().info("AllyRadar enabled with " + radarProvider.implementationName() + ".");
    }

    private VisibilityMode visibilityFromConfig() {
        try {
            return VisibilityMode.parse(getConfig().getString("default-location-visibility", "TEAM"));
        } catch (IllegalArgumentException exception) {
            getLogger().warning("Invalid default-location-visibility; using TEAM.");
            return VisibilityMode.TEAM;
        }
    }

    private void registerCommand(final String name, final AllyRadarCommand command) {
        final PluginCommand pluginCommand = getCommand(name);
        if (pluginCommand == null) {
            throw new IllegalStateException("Missing command in plugin.yml: " + name);
        }
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
    }

    @Override
    public void onDisable() {
        if (radarTask != null) {
            radarTask.cancel();
        }
        if (locatorBarFilter != null) {
            locatorBarFilter.unregister();
            locatorBarFilter = null;
        }
        if (radarProvider != null) {
            getServer().getOnlinePlayers().forEach(radarProvider::clear);
        }
        for (final Player player : getServer().getOnlinePlayers()) {
            restoreDefaultAttributes(player);
        }
        if (repository != null) {
            repository.close();
        }
        preferenceCache.clear();
        teamCache.clear();
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        preferences(event.getPlayer());
        updatePlayerAttributes(event.getPlayer());
        invalidateAndRefreshAll();
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        if (radarProvider != null) {
            radarProvider.clear(event.getPlayer());
        }
        restoreDefaultAttributes(event.getPlayer());
        preferenceCache.remove(event.getPlayer().getUniqueId());
        teamCache.remove(event.getPlayer().getUniqueId());
        if (isEnabled()) {
            getServer().getScheduler().runTask(this, this::invalidateAndRefreshAll);
        }
    }

    @EventHandler
    public void onWorldChange(final PlayerChangedWorldEvent event) {
        if (radarProvider != null) {
            radarProvider.clear(event.getPlayer());
        }
        updatePlayerAttributes(event.getPlayer());
        invalidateAndRefreshAll();
    }

    public void invalidateAndRefreshAll() {
        clearCaches();
        if (isEnabled()) {
            for (final Player player : getServer().getOnlinePlayers()) {
                updatePlayerAttributes(player);
                if (radarProvider != null) {
                    radarProvider.render(player);
                }
            }
        }
    }

    public void updatePlayerAttributes(final Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        final PlayerPreferences prefs = preferences(player);
        final Optional<TeamSummary> team = team(player);
        final VisibilityMode effective = RadarPolicy.effectiveVisibility(prefs, team);

        // 1. Waypoint Transmit Range: Controls whether this player is broadcast on anyone's Locator Bar
        try {
            final AttributeInstance transmit = player.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE);
            if (transmit != null) {
                if (effective == VisibilityMode.PRIVATE) {
                    transmit.setBaseValue(0.0);
                } else if (effective == VisibilityMode.PUBLIC) {
                    transmit.setBaseValue(60000000.0);
                } else { // TEAM
                    // If ProtocolLib packet filter is active, allow broadcast so ProtocolLib filters per-recipient.
                    // If no packet filter is active, setting transmit > 0 leaks to enemies on vanilla! So fail-safe to 0.0.
                    if (isLocatorBarFilterActive()) {
                        transmit.setBaseValue(60000000.0);
                    } else {
                        transmit.setBaseValue(0.0);
                    }
                }
            }
        } catch (final Throwable ignored) {
        }

        // 2. Waypoint Receive Range: Controls whether this player receives any waypoints on their Locator Bar
        try {
            final AttributeInstance receive = player.getAttribute(Attribute.WAYPOINT_RECEIVE_RANGE);
            if (receive != null) {
                if (!prefs.radarEnabled()) {
                    receive.setBaseValue(0.0);
                } else {
                    receive.setBaseValue(60000000.0);
                }
            }
        } catch (final Throwable ignored) {
        }
    }

    public void restoreDefaultAttributes(final Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        try {
            final AttributeInstance transmit = player.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE);
            if (transmit != null) {
                transmit.setBaseValue(60000000.0);
            }
            final AttributeInstance receive = player.getAttribute(Attribute.WAYPOINT_RECEIVE_RANGE);
            if (receive != null) {
                receive.setBaseValue(60000000.0);
            }
        } catch (final Throwable ignored) {
        }
    }

    public boolean isLocatorBarFilterActive() {
        return locatorBarFilter != null && locatorBarFilter.isActive();
    }

    public void reloadRadarConfig() {
        maximumTeamSize = Math.max(2, getConfig().getInt("max-team-size", 10));
        invitationExpirationHours = Math.max(1L, getConfig().getLong("invitation-expiration-hours", 48L));
        radarMaxDistance = Math.max(1.0, getConfig().getDouble("radar-max-distance", 1000.0));
        if (radarProvider != null) {
            getServer().getOnlinePlayers().forEach(radarProvider::clear);
        }
        radarProvider = createRadarProvider(getConfig().getString("radar-display-mode", "ACTION_BAR"));
        invalidateAndRefreshAll();
    }

    public RadarCompatibilityProvider createRadarProvider(final String mode) {
        final boolean publicRadar = getConfig().getBoolean("public-radar-enabled", true);
        final String normalized = mode == null ? "ACTION_BAR" : mode.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "BOSS_BAR" -> new BossBarRadarProvider(this, this, radarMaxDistance, publicRadar);
            case "BOTH" -> new CompositeRadarProvider(List.of(
                    new ActionBarRadarProvider(this, this, radarMaxDistance, publicRadar),
                    new BossBarRadarProvider(this, this, radarMaxDistance, publicRadar)
            ));
            case "DISABLED" -> new DisabledRadarProvider();
            case "ACTION_BAR" -> new ActionBarRadarProvider(this, this, radarMaxDistance, publicRadar);
            default -> {
                getLogger().warning("Unknown radar-display-mode: " + mode + "; defaulting to ACTION_BAR.");
                yield new ActionBarRadarProvider(this, this, radarMaxDistance, publicRadar);
            }
        };
    }

    public AllyRepository repository() {
        return repository;
    }

    public RadarCompatibilityProvider radarProvider() {
        return radarProvider;
    }

    public int maximumTeamSize() {
        return maximumTeamSize;
    }

    public long invitationExpirationHours() {
        return invitationExpirationHours;
    }

    public double radarMaxDistance() {
        return radarMaxDistance;
    }

    public void clearCaches() {
        preferenceCache.clear();
        teamCache.clear();
    }

    @Override
    public PlayerPreferences preferences(final Player player) {
        return preferenceCache.computeIfAbsent(player.getUniqueId(), repository::preferences);
    }

    @Override
    public Optional<TeamSummary> team(final Player player) {
        return teamCache.computeIfAbsent(player.getUniqueId(), repository::teamOf);
    }

    @Override
    public boolean sameTeam(final Player first, final Player second) {
        final Optional<TeamSummary> firstTeam = team(first);
        final Optional<TeamSummary> secondTeam = team(second);
        return firstTeam.isPresent() && secondTeam.isPresent()
                && firstTeam.get().id() == secondTeam.get().id();
    }
}
