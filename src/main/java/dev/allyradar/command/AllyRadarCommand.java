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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class AllyRadarCommand implements CommandExecutor, TabCompleter {
    private final AllyRadar plugin;

    public AllyRadarCommand(final AllyRadar plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        try {
            if (!sender.hasPermission("allyradar.use")) {
                sender.sendMessage("[AllyRadar] You do not have permission.");
                return true;
            }
            final String cmd = label.toLowerCase(Locale.ROOT);
            if (cmd.equals("team")) {
                return team(sender, args);
            }
            if (cmd.equals("radar")) {
                return radar(sender, args);
            }
            return ally(sender, args);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            sender.sendMessage("[AllyRadar] " + exception.getMessage());
            return true;
        }
    }

    private boolean ally(final CommandSender sender, final String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendGeneralHelp(sender);
            return true;
        }
        final String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("reload")) {
            return reload(sender);
        }
        if (sub.equals("team")) {
            final String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
            return team(sender, subArgs);
        }
        if (sub.equals("radar")) {
            final String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
            return radar(sender, subArgs);
        }
        // Direct shortcuts for team commands: /ally create, invite, accept, deny, leave, kick, transfer, info, list, disband, settings
        if (List.of("create", "invite", "accept", "deny", "leave", "kick", "transfer", "info", "list", "disband", "settings").contains(sub)) {
            return team(sender, args);
        }
        // Direct shortcuts for radar commands: /ally on, off, private, team, public, visibility, status, who
        if (List.of("on", "off", "private", "public", "visibility", "status", "who").contains(sub)) {
            return radar(sender, args);
        }
        sendGeneralHelp(sender);
        return true;
    }

    private boolean reload(final CommandSender sender) {
        if (!sender.hasPermission("allyradar.admin")) {
            sender.sendMessage("[AllyRadar] You do not have permission.");
            return true;
        }
        plugin.reloadConfig();
        plugin.reloadRadarConfig();
        sender.sendMessage("[AllyRadar] Configuration reloaded. Restart to apply radar interval or database changes.");
        return true;
    }

    private void sendGeneralHelp(final CommandSender sender) {
        sender.sendMessage("[AllyRadar] ===== AllyRadar Help =====");
        sender.sendMessage("[AllyRadar] /ally create <name> - Create a team");
        sender.sendMessage("[AllyRadar] /ally invite <player> - Invite a player");
        sender.sendMessage("[AllyRadar] /ally accept [team] - Accept team invitation");
        sender.sendMessage("[AllyRadar] /ally deny [team] - Decline team invitation");
        sender.sendMessage("[AllyRadar] /ally leave - Leave your team");
        sender.sendMessage("[AllyRadar] /ally list - List team members");
        sender.sendMessage("[AllyRadar] /ally info - View team details");
        sender.sendMessage("[AllyRadar] /ally on / off - Turn radar HUD on/off");
        sender.sendMessage("[AllyRadar] /ally private - Hide your location from everyone");
        sender.sendMessage("[AllyRadar] /ally visibility <private|team|public> - Change sharing");
        sender.sendMessage("[AllyRadar] /ally status - View radar and team status");
        sender.sendMessage("[AllyRadar] (All commands also work via /team and /radar)");
    }

    private boolean team(final CommandSender sender, final String[] args) {
        final Player player = player(sender);
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendTeamHelp(player);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> {
                requireArgs(args, 2);
                final TeamSummary team = plugin.repository().createTeam(player.getUniqueId(), player.getName(), join(args, 1), plugin.maximumTeamSize());
                plugin.invalidateAndRefreshAll();
                player.sendMessage("[AllyRadar] Created team " + team.name() + "!");
            }
            case "invite" -> {
                requireArgs(args, 2);
                final String targetArg = args[1];
                final Player onlineTarget = resolveOnlinePlayer(targetArg);
                final UUID targetUuid = onlineTarget != null ? onlineTarget.getUniqueId() : offline(targetArg).getUniqueId();
                final String targetName = onlineTarget != null ? onlineTarget.getName() : targetArg;
                final Optional<TeamSummary> myTeam = plugin.team(player);
                final String teamName = myTeam.map(TeamSummary::name).orElse("your team");

                plugin.repository().invite(player.getUniqueId(), targetUuid, targetName,
                        Duration.ofHours(plugin.invitationExpirationHours()),
                        plugin.maximumTeamSize());
                player.sendMessage("[AllyRadar] Invitation sent to " + targetName + ".");
                if (onlineTarget != null && onlineTarget.isOnline()) {
                    onlineTarget.sendMessage("[AllyRadar] You have been invited to join team '" + teamName + "' by " + player.getName() + "!");
                    onlineTarget.sendMessage("[AllyRadar] Type /team accept or /ally accept to join!");
                }
            }
            case "accept" -> {
                final String teamArg = args.length >= 2 ? join(args, 1) : "";
                final TeamSummary team = plugin.repository().accept(player.getUniqueId(), teamArg, player.getName(), plugin.maximumTeamSize());
                plugin.invalidateAndRefreshAll();
                player.sendMessage("[AllyRadar] Joined team " + team.name() + "!");
            }
            case "deny" -> {
                final String teamArg = args.length >= 2 ? join(args, 1) : "";
                plugin.repository().deny(player.getUniqueId(), teamArg, player.getName());
                player.sendMessage("[AllyRadar] Invitation declined.");
            }
            case "leave" -> {
                plugin.repository().leave(player.getUniqueId());
                plugin.invalidateAndRefreshAll();
                player.sendMessage("[AllyRadar] You left your team.");
            }
            case "kick" -> {
                requireArgs(args, 2);
                final Player onlineTarget = resolveOnlinePlayer(args[1]);
                final UUID targetUuid = onlineTarget != null ? onlineTarget.getUniqueId() : offline(args[1]).getUniqueId();
                plugin.repository().kick(player.getUniqueId(), targetUuid);
                plugin.invalidateAndRefreshAll();
                player.sendMessage("[AllyRadar] Player removed from the team.");
            }
            case "transfer" -> {
                requireArgs(args, 2);
                final Player onlineTarget = resolveOnlinePlayer(args[1]);
                final UUID targetUuid = onlineTarget != null ? onlineTarget.getUniqueId() : offline(args[1]).getUniqueId();
                plugin.repository().transfer(player.getUniqueId(), targetUuid);
                plugin.invalidateAndRefreshAll();
                player.sendMessage("[AllyRadar] Ownership transferred.");
            }
            case "info" -> teamInfo(player);
            case "list" -> teamList(player);
            case "disband" -> {
                plugin.repository().disband(player.getUniqueId());
                plugin.invalidateAndRefreshAll();
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
            final Optional<TeamSummary> team = plugin.team(player);
            player.sendMessage("[AllyRadar] Radar receiving: " + (preferences.radarEnabled() ? "ON" : "OFF")
                    + " | Location sharing: " + preferences.visibility()
                    + " | Team: " + team.map(TeamSummary::name).orElse("None"));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "on" -> {
                plugin.repository().setRadarEnabled(player.getUniqueId(), true);
                plugin.invalidateAndRefreshAll();
                player.sendMessage("[AllyRadar] Radar receiving enabled.");
            }
            case "off" -> {
                plugin.repository().setRadarEnabled(player.getUniqueId(), false);
                plugin.radarProvider().clear(player);
                plugin.invalidateAndRefreshAll();
                player.sendMessage("[AllyRadar] Radar receiving disabled. (To hide yourself from teammates, use /radar private or /ally private)");
            }
            case "private" -> {
                plugin.repository().setVisibility(player.getUniqueId(), VisibilityMode.PRIVATE);
                plugin.invalidateAndRefreshAll();
                player.sendMessage("[AllyRadar] Location sharing set to PRIVATE. You are hidden from all player radars.");
            }
            case "public" -> {
                plugin.repository().setVisibility(player.getUniqueId(), VisibilityMode.PUBLIC);
                plugin.invalidateAndRefreshAll();
                player.sendMessage("[AllyRadar] Location sharing set to PUBLIC.");
            }
            case "visibility" -> {
                if (args.length < 2) {
                    player.sendMessage("[AllyRadar] Use /radar visibility <private|team|public> (or /radar private).");
                    return true;
                }
                final String modeArg = args[1].toLowerCase(Locale.ROOT);
                final VisibilityMode mode = switch (modeArg) {
                    case "off", "hide", "private" -> VisibilityMode.PRIVATE;
                    case "on", "team" -> VisibilityMode.TEAM;
                    case "public" -> VisibilityMode.PUBLIC;
                    default -> VisibilityMode.parse(args[1]);
                };
                plugin.repository().setVisibility(player.getUniqueId(), mode);
                plugin.invalidateAndRefreshAll();
                player.sendMessage("[AllyRadar] Location sharing set to " + mode + "."
                        + (mode == VisibilityMode.PRIVATE ? " You are hidden from all player radars." : ""));
            }
            case "who" -> radarWho(player);
            default -> player.sendMessage("[AllyRadar] Use /radar on, off, private, visibility <private|team|public>, status, or who.");
        }
        return true;
    }

    private void radarWho(final Player viewer) {
        final PlayerPreferences viewerPreferences = plugin.preferences(viewer);
        final Optional<TeamSummary> viewerTeam = plugin.team(viewer);
        final List<String> names = plugin.getServer().getOnlinePlayers().stream()
                .filter(target -> !target.equals(viewer) && target.getWorld().equals(viewer.getWorld()))
                .filter(target -> RadarPolicy.canTrack(viewer.getUniqueId(), target.getUniqueId(), viewerPreferences,
                        plugin.preferences(target), viewerTeam, plugin.team(target),
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
        final boolean enabled = switch (args[2].toLowerCase(Locale.ROOT)) {
            case "on" -> true;
            case "off" -> false;
            default -> throw new IllegalArgumentException("Use on or off.");
        };
        plugin.repository().setTeamRadar(player.getUniqueId(), enabled);
        plugin.invalidateAndRefreshAll();
        player.sendMessage("[AllyRadar] Team radar " + (enabled ? "enabled" : "disabled") + ". Individual PRIVATE settings still override it.");
    }

    private void sendTeamHelp(final Player player) {
        player.sendMessage("[AllyRadar] /team create <name>, invite <player>, accept [team], deny [team], leave");
        player.sendMessage("[AllyRadar] /team kick <player>, transfer <player>, info, list, settings radar <on|off>, disband");
    }

    private Player player(final CommandSender sender) {
        if (!(sender instanceof Player player)) {
            throw new IllegalArgumentException("This command must be run by a player.");
        }
        return player;
    }

    private Player resolveOnlinePlayer(final String name) {
        final Player exact = plugin.getServer().getPlayerExact(name);
        return exact != null ? exact : plugin.getServer().getPlayer(name);
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
        return String.join(" ", Arrays.copyOfRange(args, start, args.length));
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        final String main = alias.toLowerCase(Locale.ROOT);
        final List<String> values;
        if (main.equals("ally") || main.equals("allyradar") || main.equals("ar")) {
            if (args.length == 1) {
                values = List.of("help", "create", "invite", "accept", "deny", "leave", "kick", "transfer", "info", "list", "disband", "settings", "radar", "on", "off", "private", "status", "visibility", "who", "reload");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("settings")) {
                values = List.of("radar");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("visibility")) {
                values = List.of("private", "team", "public");
            } else {
                values = List.of();
            }
        } else if (main.equals("team")) {
            values = args.length == 1
                    ? List.of("create", "invite", "accept", "deny", "leave", "kick", "transfer", "info", "list", "settings", "disband")
                    : args.length == 2 && args[0].equalsIgnoreCase("settings") ? List.of("radar") : List.of();
        } else if (main.equals("radar")) {
            values = args.length == 1 ? List.of("on", "off", "private", "visibility", "status", "who")
                    : args.length == 2 && args[0].equalsIgnoreCase("visibility") ? List.of("private", "team", "public") : List.of();
        } else {
            values = List.of("reload");
        }
        final String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.startsWith(prefix)).sorted().toList();
    }
}
