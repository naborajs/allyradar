package dev.allyradar;

import dev.allyradar.command.AllyRadarCommand;
import dev.allyradar.model.PlayerPreferences;
import dev.allyradar.model.TeamSummary;
import dev.allyradar.model.VisibilityMode;
import dev.allyradar.radar.ActionBarRadarProvider;
import dev.allyradar.radar.RadarCompatibilityProvider;
import dev.allyradar.storage.AllyRepository;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AllyRadar extends JavaPlugin implements Listener, ActionBarRadarProvider.AllyRadarAccess {
    private AllyRepository repository;
    private RadarCompatibilityProvider radarProvider;
    private BukkitTask radarTask;
    private int maximumTeamSize;
    private long invitationExpirationHours;
    private long radarUpdateIntervalTicks;
    private double radarMaxDistance;
    private final Map<UUID, PlayerPreferences> preferenceCache = new HashMap<>();
    private final Map<UUID, Optional<TeamSummary>> teamCache = new HashMap<>();

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
        radarProvider = new ActionBarRadarProvider(this, this,
                radarMaxDistance,
                getConfig().getBoolean("public-radar-enabled", false));
        final AllyRadarCommand command = new AllyRadarCommand(this);
        registerCommand("allyradar", command);
        registerCommand("team", command);
        registerCommand("radar", command);
        getServer().getPluginManager().registerEvents(this, this);
        radarTask = getServer().getScheduler().runTaskTimer(this, () ->
                getServer().getOnlinePlayers().forEach(radarProvider::render),
                radarUpdateIntervalTicks, radarUpdateIntervalTicks);
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
        if (radarProvider != null) {
            getServer().getOnlinePlayers().forEach(radarProvider::clear);
        }
        if (repository != null) {
            repository.close();
        }
        preferenceCache.clear();
        teamCache.clear();
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        clearCaches();
        preferences(event.getPlayer());
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        radarProvider.clear(event.getPlayer());
        preferenceCache.remove(event.getPlayer().getUniqueId());
        teamCache.remove(event.getPlayer().getUniqueId());
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
