package dev.allyradar.command;

import dev.allyradar.AllyRadar;
import dev.allyradar.model.PlayerPreferences;
import dev.allyradar.model.TeamMember;
import dev.allyradar.model.TeamSummary;
import dev.allyradar.model.VisibilityMode;
import dev.allyradar.radar.RadarPolicy;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class AllyRadarCommand implements CommandExecutor, TabCompleter {
    private final AllyRadar plugin;

    public AllyRadarCommand(final AllyRadar plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        try {
            if ((label.equalsIgnoreCase("team") || label.equalsIgnoreCase("radar"))
                    && !sender.hasPermission("allyradar.use")) {
                sender.sendMessage("[AllyRadar] You do not have permission.");
                return true;
            }
            if (label.equalsIgnoreCase("team")) {
                return team(sender, args);
            }
            if (label.equalsIgnoreCase("radar")) {
                return radar(sender, args);
            }
            return root(sender, args);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            sender.sendMessage("[AllyRadar] " + exception.getMessage());
            return true;
        }
    }

    private boolean root(final CommandSender sender, final String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("allyradar.admin")) {
                sender.sendMessage("[AllyRadar] You do not have permission.");
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage("[AllyRadar] Configuration reloaded. Restart to apply radar interval or database changes.");
            return true;
        }
        sender.sendMessage("[AllyRadar] Use /team or /radar. Radar uses filtered action-bar output; exact coordinates are never displayed.");
        return true;
    }

    private boolean team(final CommandSender sender, final String[] args) {
        final Player player = player(sender);
        if (args.length == 0) {
            sendTeamHelp(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "create" -> {
                requireArgs(args, 2);
                final TeamSummary team = plugin.repository().createTeam(player.getUniqueId(), join(args, 1), plugin.maximumTeamSize());
                plugin.clearCaches();
                player.sendMessage("[AllyRadar] Created team " + team.name() + ".");
            }
            case "invite" -> {
                requireArgs(args, 2);
                final OfflinePlayer target = plugin.getServer().getOfflinePlayer(args[1]);
                plugin.repository().invite(player.getUniqueId(), target.getUniqueId(), args[1],
                        Duration.ofHours(plugin.invitationExpirationHours()),
                        plugin.maximumTeamSize());
                player.sendMessage("[AllyRadar] Invitation sent to " + args[1] + ".");
            }
            case "accept" -> {
                requireArgs(args, 2);
                final TeamSummary team = plugin.repository().accept(player.getUniqueId(), join(args, 1), player.getName(), plugin.maximumTeamSize());
                plugin.clearCaches();
                player.sendMessage("[AllyRadar] Joined " + team.name() + ".");
            }
            case "deny" -> {
                requireArgs(args, 2);
                plugin.repository().deny(player.getUniqueId(), join(args, 1));
                player.sendMessage("[AllyRadar] Invitation declined.");
            }
            case "leave" -> {
                plugin.repository().leave(player.getUniqueId());
                plugin.clearCaches();
                player.sendMessage("[AllyRadar] You left your team.");
            }
            case "kick" -> {
                requireArgs(args, 2);
                plugin.repository().kick(player.getUniqueId(), offline(args[1]).getUniqueId());
                plugin.clearCaches();
                player.sendMessage("[AllyRadar] Player removed from the team.");
            }
            case "transfer" -> {
                requireArgs(args, 2);
                plugin.repository().transfer(player.getUniqueId(), offline(args[1]).getUniqueId());
                plugin.clearCaches();
                player.sendMessage("[AllyRadar] Ownership transferred.");
            }
            case "info" -> teamInfo(player);
            case "list" -> teamList(player);
            case "disband" -> {
                plugin.repository().disband(player.getUniqueId());
                plugin.clearCaches();
                player.sendMessage("[AllyRadar] Team disbanded.");
            }
            case "settings" -> teamSettings(player, args);
            default -> sendTeamHelp(player);
        }
        return true;
    }

    private boolean radar(final CommandSender sender, final String[] args) {
        final Player player = player(sender);
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            final PlayerPreferences preferences = plugin.preferences(player);
            player.sendMessage("[AllyRadar] Sharing: " + preferences.visibility() + ", receiving radar: "
                    + (preferences.radarEnabled() ? "ON" : "OFF") + ".");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "on" -> {
                plugin.repository().setRadarEnabled(player.getUniqueId(), true);
                plugin.clearCaches();
                player.sendMessage("[AllyRadar] Radar receiving enabled.");
            }
            case "off" -> {
                plugin.repository().setRadarEnabled(player.getUniqueId(), false);
                plugin.clearCaches();
                player.sendMessage("[AllyRadar] Radar receiving disabled.");
            }
            case "visibility" -> {
                requireArgs(args, 2);
                final VisibilityMode mode = VisibilityMode.parse(args[1]);
                plugin.repository().setVisibility(player.getUniqueId(), mode);
                plugin.clearCaches();
                player.sendMessage("[AllyRadar] Location sharing set to " + mode + ".");
            }
            case "who" -> radarWho(player);
            default -> player.sendMessage("[AllyRadar] Use /radar on, off, visibility <private|team|public>, status, or who.");
        }
        return true;
    }

    private void radarWho(final Player viewer) {
        final PlayerPreferences viewerPreferences = plugin.preferences(viewer);
        final Optional<TeamSummary> viewerTeam = plugin.team(viewer);
        final List<String> names = plugin.getServer().getOnlinePlayers().stream()
                .filter(target -> !target.equals(viewer) && target.getWorld().equals(viewer.getWorld()))
                .filter(target -> RadarPolicy.canTrack(viewer.getUniqueId(), target.getUniqueId(), viewerPreferences,
                        plugin.preferences(target), viewerTeam.isPresent() && plugin.sameTeam(viewer, target),
                        viewerTeam.map(TeamSummary::radarEnabled).orElse(false),
                        plugin.getConfig().getBoolean("public-radar-enabled", false)))
                .map(Player::getName).sorted().toList();
        viewer.sendMessage(names.isEmpty() ? "[AllyRadar] No authorized players are visible." : "[AllyRadar] Visible: " + String.join(", ", names));
    }

    private void teamInfo(final Player player) {
        final TeamSummary team = plugin.team(player).orElseThrow(() -> new IllegalArgumentException("You are not in a team."));
        player.sendMessage("[AllyRadar] " + team.name() + " — " + team.memberCount() + "/" + plugin.maximumTeamSize()
                + ", team radar " + (team.radarEnabled() ? "ON" : "OFF") + ".");
    }

    private void teamList(final Player player) {
        final List<TeamMember> members = plugin.repository().members(player.getUniqueId());
        player.sendMessage("[AllyRadar] Members: " + members.stream().map(member ->
                member.lastKnownName() + (member.owner() ? " (owner)" : "")
                        + (plugin.getServer().getPlayer(member.playerId()) != null ? " [online]" : " [offline]")
        ).collect(Collectors.joining(", ")));
    }

    private void teamSettings(final Player player, final String[] args) {
        requireArgs(args, 3);
        if (!args[1].equalsIgnoreCase("radar")) {
            throw new IllegalArgumentException("Use /team settings radar <on|off>.");
        }
        final boolean enabled = switch (args[2].toLowerCase()) {
            case "on" -> true;
            case "off" -> false;
            default -> throw new IllegalArgumentException("Use on or off.");
        };
        plugin.repository().setTeamRadar(player.getUniqueId(), enabled);
        plugin.clearCaches();
        player.sendMessage("[AllyRadar] Team radar " + (enabled ? "enabled" : "disabled") + ". Individual PRIVATE settings still override it.");
    }

    private void sendTeamHelp(final Player player) {
        player.sendMessage("[AllyRadar] /team create <name>, invite <player>, accept <team>, deny <team>, leave");
        player.sendMessage("[AllyRadar] /team kick <player>, transfer <player>, info, list, settings radar <on|off>, disband");
    }

    private Player player(final CommandSender sender) {
        if (!(sender instanceof Player player)) {
            throw new IllegalArgumentException("This command must be run by a player.");
        }
        return player;
    }

    private OfflinePlayer offline(final String name) {
        return plugin.getServer().getOfflinePlayer(name);
    }

    private void requireArgs(final String[] args, final int count) {
        if (args.length < count) {
            throw new IllegalArgumentException("Not enough arguments.");
        }
    }

    private String join(final String[] args, final int start) {
        return String.join(" ", java.util.Arrays.copyOfRange(args, start, args.length));
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        final List<String> values = switch (alias.toLowerCase()) {
            case "team" -> args.length == 1
                    ? List.of("create", "invite", "accept", "deny", "leave", "kick", "transfer", "info", "list", "settings", "disband")
                    : args.length == 2 && args[0].equalsIgnoreCase("settings") ? List.of("radar") : List.of();
            case "radar" -> args.length == 1 ? List.of("on", "off", "visibility", "status", "who")
                    : args.length == 2 && args[0].equalsIgnoreCase("visibility") ? List.of("private", "team", "public") : List.of();
            default -> List.of("reload");
        };
        final String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
        return values.stream().filter(value -> value.startsWith(prefix)).sorted().toList();
    }
}
