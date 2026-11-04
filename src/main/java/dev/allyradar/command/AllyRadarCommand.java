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
        // Direct shortcuts for team commands
        if (List.of("create", "invite", "accept", "join", "deny", "leave", "kick", "transfer", "info", "list", "disband", "delete", "settings").contains(sub)) {
            return team(sender, args);
        }
        // Direct shortcuts for radar commands
        if (List.of("on", "off", "private", "hide", "public", "visibility", "status", "who", "solo", "hud", "radius", "range", "bedrock", "b").contains(sub)) {
            return radar(sender, args);
        }
        if (sub.equals("team")) {
            if (args.length > 1) {
                final String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                return team(sender, subArgs);
            }
            return radar(sender, new String[]{"team"});
        }
        if (sub.equals("radar")) {
            sender.sendMessage("§b[AllyRadar] §7To turn your radar on or off, use §f/ally on §7or §f/ally off§7.");
            sender.sendMessage("§7(Type §f/ally help §7to see all available commands).");
            return true;
        }
        sendGeneralHelp(sender);
        return true;
    }

    private boolean reload(final CommandSender sender) {
        if (!sender.hasPermission("allyradar.admin")) {
            sender.sendMessage("§c[AllyRadar] You do not have permission.");
            return true;
        }
        plugin.reloadConfig();
        plugin.reloadRadarConfig();
        sender.sendMessage("§a[AllyRadar] Configuration reloaded.");
        return true;
    }

    private void sendGeneralHelp(final CommandSender sender) {
        final boolean isBedrock = sender instanceof Player p && p.getName().startsWith(".");
        sender.sendMessage("§b[AllyRadar] §l=== AllyRadar Commands ===");
        sender.sendMessage("§e► Radar & Screen Display:");
        sender.sendMessage(" §f/ally on          §7- Turn ON radar & team location sharing");
        sender.sendMessage(" §f/ally off         §7- Turn OFF radar & hide location completely");
        sender.sendMessage(" §f/ally hud [on|off]§7- Toggle/turn off the text at bottom of screen");
        sender.sendMessage(" §f/ally radius <m>  §7- Set custom radar distance (e.g. §f/ally radius 50§7)");
        sender.sendMessage("§e► Privacy & Visibility:");
        sender.sendMessage(" §f/ally private     §7- Hide your location from EVERYONE (100% invisible)");
        sender.sendMessage(" §f/ally team        §7- Show location ONLY to your teammates");
        sender.sendMessage(" §f/ally public      §7- Share location with everyone (solo players)");
        sender.sendMessage("§e► Team Commands:");
        sender.sendMessage(" §f/ally create <name>   §7- Create a new team");
        sender.sendMessage(" §f/ally invite <player> §7- Invite a player to your team");
        sender.sendMessage(" §f/ally accept          §7- Accept a team invitation");
        sender.sendMessage(" §f/ally deny            §7- Decline a team invitation");
        sender.sendMessage(" §f/ally leave           §7- Leave your team (deletes if sole member)");
        sender.sendMessage(" §f/ally list            §7- View all members in your team");
        sender.sendMessage(" §f/ally disband         §7- Delete your team (owner only)");
        sender.sendMessage("§e► Status & Guides:");
        sender.sendMessage(" §f/ally status          §7- View your team, radius, and radar status");
        sender.sendMessage(" §f/ally bedrock         §7- Bedrock touch & controller quick shortcuts (§f/a§7)");
        if (isBedrock) {
            sender.sendMessage("§a[Bedrock Tip] §7You can use §f/a §7instead of /ally (e.g. §f/a on§7, §f/a hud§7)!");
        }
    }

    private void sendBedrockHelp(final CommandSender sender) {
        sender.sendMessage("§b[AllyRadar] §l=== Bedrock Touch & Controller Guide ===");
        sender.sendMessage("§eUse §f/a §einstead of §f/ally §eto type fast on mobile/console:");
        sender.sendMessage(" §f/a on          §7- Turn ON radar & team sharing");
        sender.sendMessage(" §f/a off         §7- Turn OFF radar & hide completely");
        sender.sendMessage(" §f/a hud         §7- Toggle bottom-screen text on/off");
        sender.sendMessage(" §f/a radius 50   §7- Set radar distance to 50 blocks");
        sender.sendMessage(" §f/a radius reset§7- Reset radar distance to server maximum");
        sender.sendMessage(" §f/a public      §7- Share location with everyone (solo)");
        sender.sendMessage(" §f/a private     §7- Hide location (100% invisible)");
        sender.sendMessage(" §f/a accept      §7- Join team that invited you");
        sender.sendMessage(" §f/a leave       §7- Leave your current team");
        sender.sendMessage(" §f/a status      §7- Check who can see you");
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
                player.sendMessage("§b[AllyRadar] §aCreated team '" + team.name() + "'! §7Invite players using §f/ally invite <player>§7.");
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
                player.sendMessage("§b[AllyRadar] §aInvitation sent to " + targetName + ". §7Waiting for them to accept.");
                if (onlineTarget != null && onlineTarget.isOnline()) {
                    onlineTarget.sendMessage("§b[AllyRadar] §e" + player.getName() + " §7invited you to join team §e'" + teamName + "'§7!");
                    onlineTarget.sendMessage("§b[AllyRadar] §7Type §f/ally accept §7to join, or §f/ally deny §7to decline.");
                }
            }
            case "accept", "join" -> {
                final String teamArg = args.length >= 2 ? join(args, 1) : "";
                final TeamSummary team = plugin.repository().accept(player.getUniqueId(), teamArg, player.getName(), plugin.maximumTeamSize());
                plugin.invalidateAndRefreshAll();
                player.sendMessage("§b[AllyRadar] §aJoined team '" + team.name() + "'! §7You can now see your teammates on your radar.");
            }
            case "deny", "decline" -> {
                final String teamArg = args.length >= 2 ? join(args, 1) : "";
                plugin.repository().deny(player.getUniqueId(), teamArg, player.getName());
                player.sendMessage("§b[AllyRadar] §7Invitation declined.");
            }
            case "leave" -> {
                final Optional<TeamSummary> teamOpt = plugin.team(player);
                if (teamOpt.isEmpty()) {
                    player.sendMessage("§b[AllyRadar] §7You are not currently in a team.");
                    return true;
                }
                final TeamSummary team = teamOpt.get();
                final boolean wasOwner = team.owner().equals(player.getUniqueId());
                final boolean wasSole = team.memberCount() <= 1;
                plugin.repository().leave(player.getUniqueId());
                plugin.invalidateAndRefreshAll();
                if (wasOwner && wasSole) {
                    player.sendMessage("§b[AllyRadar] §aTeam disbanded. §7You are now playing solo.");
                } else {
                    player.sendMessage("§b[AllyRadar] §aYou left team '" + team.name() + "'. §7You are now playing solo.");
                }
            }
            case "kick" -> {
                requireArgs(args, 2);
                final Player onlineTarget = resolveOnlinePlayer(args[1]);
                final UUID targetUuid = onlineTarget != null ? onlineTarget.getUniqueId() : offline(args[1]).getUniqueId();
                plugin.repository().kick(player.getUniqueId(), targetUuid);
                plugin.invalidateAndRefreshAll();
                player.sendMessage("§b[AllyRadar] §aPlayer removed from the team.");
            }
            case "transfer" -> {
                requireArgs(args, 2);
                final Player onlineTarget = resolveOnlinePlayer(args[1]);
                final UUID targetUuid = onlineTarget != null ? onlineTarget.getUniqueId() : offline(args[1]).getUniqueId();
                plugin.repository().transfer(player.getUniqueId(), targetUuid);
                plugin.invalidateAndRefreshAll();
                player.sendMessage("§b[AllyRadar] §aTeam ownership transferred.");
            }
            case "info" -> teamInfo(player);
            case "list" -> teamList(player);
            case "disband", "delete" -> {
                plugin.repository().disband(player.getUniqueId());
                plugin.invalidateAndRefreshAll();
                player.sendMessage("§b[AllyRadar] §aTeam disbanded/deleted. §7All members are now playing solo.");
            }
            case "settings" -> teamSettings(player, args);
            default -> sendTeamHelp(player);
        }
        return true;
    }

    private boolean radar(final CommandSender sender, final String[] args) {
        final Player player = player(sender);
        final Optional<TeamSummary> team = plugin.team(player);
        final boolean isSolo = team.isEmpty();
        final PlayerPreferences preferences = plugin.preferences(player);

        if (args.length == 0 || args[0].equalsIgnoreCase("status") || args[0].equalsIgnoreCase("solo")) {
            final VisibilityMode effective = RadarPolicy.effectiveVisibility(preferences, team);

            player.sendMessage("§b[AllyRadar] §l=== Your Radar Status ===");
            if (isSolo) {
                player.sendMessage("§7• Your Team: §fNone (Playing Solo)");
                if (effective == VisibilityMode.PUBLIC) {
                    player.sendMessage("§7• Who can see you: §aEVERYONE (Public) §7— Your location is visible to all players with radar.");
                } else {
                    player.sendMessage("§7• Who can see you: §cNOBODY (Private) §7— You are completely hidden from all players.");
                }
            } else {
                final TeamSummary t = team.get();
                player.sendMessage("§7• Your Team: §e" + t.name() + " §7(" + t.memberCount() + "/" + plugin.maximumTeamSize() + " members)");
                if (preferences.visibility() == VisibilityMode.PRIVATE) {
                    player.sendMessage("§7• Who can see you: §cNOBODY (Private) §7— You are hidden from everyone, including teammates.");
                } else if (preferences.visibility() == VisibilityMode.PUBLIC) {
                    player.sendMessage("§7• Who can see you: §aEVERYONE (Public) §7— Your location is visible to everyone on the server.");
                } else {
                    player.sendMessage("§7• Who can see you: §eTEAMMATES ONLY §7— Only members of " + t.name() + " can see you.");
                }
            }

            if (preferences.radarEnabled()) {
                player.sendMessage("§7• Radar Master: §aON §7— Radar tracking is active.");
            } else {
                player.sendMessage("§7• Radar Master: §cOFF §7— All radar displays and locator dots are disabled.");
            }
            if (preferences.hudEnabled()) {
                player.sendMessage("§7• Bottom Text HUD: §aON §7— Teammate text appears above hotbar. (§f/ally hud off§7 to hide)");
            } else {
                player.sendMessage("§7• Bottom Text HUD: §cOFF §7— Text at bottom is hidden. (§f/ally hud on§7 to show)");
            }
            final String radiusStr = preferences.customRadius() > 0.0
                    ? ((int) preferences.customRadius() + "m")
                    : ("Server Max (" + (int) plugin.radarMaxDistance() + "m)");
            player.sendMessage("§7• Detection Radius: §e" + radiusStr + " §7— (§f/ally radius <meters>§7)");
            player.sendMessage("§8(Commands: /ally hud, /ally radius, /ally on, /ally off, /ally public, /ally private)");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "on" -> {
                plugin.repository().setRadarEnabled(player.getUniqueId(), true);
                if (team.isPresent()) {
                    plugin.repository().setVisibility(player.getUniqueId(), VisibilityMode.TEAM);
                }
                plugin.invalidateAndRefreshAll();
                player.sendMessage(isSolo
                        ? "§b[AllyRadar] §aRadar display turned ON! §7Your radar screen is active. (Your location is currently "
                            + (preferences.visibility() == VisibilityMode.PUBLIC ? "§aPUBLIC" : "§cPRIVATE")
                            + "§7. To be seen by others: §f/ally public §7| To hide: §f/ally private§7)."
                        : "§b[AllyRadar] §aRadar display & location turned ON! §7You are sharing location with your §eTEAMMATES ONLY§7. Players outside your team cannot see you.");
            }
            case "off" -> {
                plugin.repository().setRadarEnabled(player.getUniqueId(), false);
                plugin.repository().setVisibility(player.getUniqueId(), VisibilityMode.PRIVATE);
                plugin.radarProvider().clear(player);
                plugin.invalidateAndRefreshAll();
                player.sendMessage("§b[AllyRadar] §cRadar turned OFF! §7All radar displays are hidden and your location is now §cHIDDEN (Private) §7from all players.");
            }
            case "private", "hide" -> {
                plugin.repository().setVisibility(player.getUniqueId(), VisibilityMode.PRIVATE);
                plugin.invalidateAndRefreshAll();
                player.sendMessage("§b[AllyRadar] §cLocation set to PRIVATE! §7You are now 100% hidden from everyone (including teammates). Nobody can see your location.");
                player.sendMessage("§7(To share with teammates: §f/ally team §7| To share with everyone: §f/ally public§7)");
            }
            case "public" -> {
                final boolean publicEnabled = plugin.getConfig().getBoolean("public-radar-enabled", true);
                plugin.repository().setVisibility(player.getUniqueId(), VisibilityMode.PUBLIC);
                plugin.invalidateAndRefreshAll();
                if (!publicEnabled) {
                    player.sendMessage("§b[AllyRadar] §eWarning: §7Public radar is currently disabled in the server config.");
                } else if (team.isPresent()) {
                    player.sendMessage("§b[AllyRadar] §aLocation set to PUBLIC (Everyone)! §7You are now visible to §eEVERYONE §7on the server (both teammates and outside players).");
                    player.sendMessage("§7(To restrict your location back to teammates only, type §f/ally team§7).");
                } else {
                    player.sendMessage("§b[AllyRadar] §aLocation set to PUBLIC (Everyone)! §7Everyone on the server with radar turned on can now see your location!");
                    player.sendMessage("§7(To hide yourself again at any time, type §f/ally private§7).");
                }
            }
            case "team" -> {
                plugin.repository().setVisibility(player.getUniqueId(), VisibilityMode.TEAM);
                plugin.invalidateAndRefreshAll();
                if (isSolo) {
                    player.sendMessage("§b[AllyRadar] §eLocation set to TEAM. §7Since you are playing solo without a team, you remain §cHIDDEN (Private) §7until you join a team.");
                    player.sendMessage("§7(To share with everyone: §f/ally public §7| To create a team: §f/ally create <name>§7)");
                } else {
                    player.sendMessage("§b[AllyRadar] §aLocation set to TEAM ONLY! §7Only your teammates in '" + team.get().name() + "' can see your location. Outside players cannot see you.");
                    player.sendMessage("§7(To share with everyone on the server: §f/ally public §7| To hide: §f/ally private§7)");
                }
            }
            case "visibility" -> {
                if (args.length < 2) {
                    player.sendMessage("§b[AllyRadar] §7Use §f/ally private§7, §f/ally team§7, or §f/ally public§7.");
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
                if (mode == VisibilityMode.PUBLIC) {
                    player.sendMessage("§b[AllyRadar] §aLocation set to PUBLIC! §7Everyone on the server can see your location.");
                } else if (mode == VisibilityMode.PRIVATE) {
                    player.sendMessage("§b[AllyRadar] §cLocation set to PRIVATE! §7You are hidden from all players.");
                } else {
                    if (isSolo) {
                        player.sendMessage("§b[AllyRadar] §eLocation set to TEAM. §7Since you have no team, you remain hidden.");
                    } else {
                        player.sendMessage("§b[AllyRadar] §aLocation set to TEAM! §7Only your teammates can see your location.");
                    }
                }
            }
            case "hud" -> {
                final boolean newHud;
                if (args.length > 1) {
                    final String toggle = args[1].toLowerCase(Locale.ROOT);
                    if (toggle.equals("on") || toggle.equals("enable") || toggle.equals("true") || toggle.equals("1")) {
                        newHud = true;
                    } else if (toggle.equals("off") || toggle.equals("disable") || toggle.equals("false") || toggle.equals("0")) {
                        newHud = false;
                    } else {
                        newHud = !preferences.hudEnabled();
                    }
                } else {
                    newHud = !preferences.hudEnabled();
                }
                plugin.repository().setHudEnabled(player.getUniqueId(), newHud);
                if (!newHud) {
                    plugin.radarProvider().clear(player);
                }
                plugin.invalidateAndRefreshAll();
                if (newHud) {
                    player.sendMessage("§b[AllyRadar] §aBottom Text HUD turned ON! §7Names and distances will display above your hotbar.");
                } else {
                    player.sendMessage("§b[AllyRadar] §cBottom Text HUD turned OFF! §7The text at the bottom of your screen is hidden. (Your XP-bar locator dots still work).");
                }
            }
            case "radius", "range" -> {
                if (args.length < 2) {
                    final String current = preferences.customRadius() > 0.0
                            ? ((int) preferences.customRadius() + "m")
                            : ("Server Max (" + (int) plugin.radarMaxDistance() + "m)");
                    player.sendMessage("§b[AllyRadar] §7Current detection radius: §e" + current);
                    player.sendMessage("§7(To change: §f/ally radius <meters> §7e.g. §f/ally radius 50 §7| Reset: §f/ally radius reset§7)");
                    return true;
                }
                final String targetArg = args[1].toLowerCase(Locale.ROOT);
                if (targetArg.equals("reset") || targetArg.equals("max") || targetArg.equals("default") || targetArg.equals("0")) {
                    plugin.repository().setCustomRadius(player.getUniqueId(), 0.0);
                    plugin.invalidateAndRefreshAll();
                    player.sendMessage("§b[AllyRadar] §aDetection radius reset to Server Max (" + (int) plugin.radarMaxDistance() + "m)!");
                    return true;
                }
                try {
                    final double radius = Double.parseDouble(args[1]);
                    if (radius <= 0.0) {
                        player.sendMessage("§b[AllyRadar] §cRadius must be greater than 0 meters.");
                        return true;
                    }
                    final double clamped = Math.min(radius, plugin.radarMaxDistance());
                    plugin.repository().setCustomRadius(player.getUniqueId(), clamped);
                    plugin.invalidateAndRefreshAll();
                    player.sendMessage("§b[AllyRadar] §aRadar detection radius set to §e" + (int) clamped + "m§7! You will only detect players within this range.");
                } catch (NumberFormatException e) {
                    player.sendMessage("§b[AllyRadar] §cInvalid number. Usage: §f/ally radius <meters> §7(e.g. §f/ally radius 50§7)");
                }
            }
            case "bedrock", "b" -> sendBedrockHelp(player);
            case "who" -> radarWho(player);
            default -> player.sendMessage("§b[AllyRadar] §7Use: §f/ally on§7, §f/ally off§7, §f/ally hud§7, §f/ally radius§7, §f/ally private§7, §f/ally team§7, §f/ally public§7, or §f/ally status§7.");
        }
        return true;
    }

    private void radarWho(final Player viewer) {
        final PlayerPreferences viewerPreferences = plugin.preferences(viewer);
        final Optional<TeamSummary> viewerTeam = plugin.team(viewer);
        final boolean publicEnabled = plugin.getConfig().getBoolean("public-radar-enabled", true);
        final double maxRange = viewerPreferences.customRadius() > 0.0
                ? Math.min(viewerPreferences.customRadius(), plugin.radarMaxDistance())
                : plugin.radarMaxDistance();
        final List<String> entries = new ArrayList<>();
        for (final Player target : plugin.getServer().getOnlinePlayers()) {
            if (target.equals(viewer) || !target.getWorld().equals(viewer.getWorld())) {
                continue;
            }
            if (RadarPolicy.canTrack(viewer.getUniqueId(), target.getUniqueId(), viewerPreferences,
                    plugin.preferences(target), viewerTeam, plugin.team(target), publicEnabled)) {
                final double distance = viewer.getLocation().distance(target.getLocation());
                if (distance <= maxRange) {
                    entries.add(target.getName() + " (" + Math.round(distance) + "m)");
                }
            }
        }
        if (entries.isEmpty()) {
            viewer.sendMessage("§b[AllyRadar] §7No visible players nearby.");
            viewer.sendMessage("§7(Players must be in your team or have §f/ally public §7enabled, with radar on).");
        } else {
            viewer.sendMessage("§b[AllyRadar] §aVisible players nearby: §f" + String.join(", ", entries));
        }
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
        if (main.equals("ally") || main.equals("allyradar") || main.equals("ar") || main.equals("a")) {
            if (args.length == 1) {
                final List<String> list = new ArrayList<>(List.of(
                        "on", "off", "hud", "radius", "private", "public", "status", "bedrock", "who",
                        "create", "invite", "accept", "deny", "leave", "list", "info", "kick", "transfer", "disband"
                ));
                if (sender.hasPermission("allyradar.admin")) {
                    list.add("reload");
                }
                values = list;
            } else if (args.length == 2 && args[0].equalsIgnoreCase("hud")) {
                values = List.of("on", "off");
            } else if (args.length == 2 && (args[0].equalsIgnoreCase("radius") || args[0].equalsIgnoreCase("range"))) {
                values = List.of("25", "50", "100", "250", "500", "reset");
            } else {
                values = List.of();
            }
        } else if (main.equals("team")) {
            values = args.length == 1
                    ? List.of("create", "invite", "accept", "deny", "leave", "list", "info", "kick", "transfer", "disband")
                    : List.of();
        } else if (main.equals("radar")) {
            values = args.length == 1
                    ? List.of("on", "off", "hud", "radius", "private", "public", "status", "who")
                    : args.length == 2 && args[0].equalsIgnoreCase("hud") ? List.of("on", "off")
                    : args.length == 2 && (args[0].equalsIgnoreCase("radius") || args[0].equalsIgnoreCase("range")) ? List.of("25", "50", "100", "250", "500", "reset")
                    : List.of();
        } else {
            values = List.of("reload");
        }
        final String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.startsWith(prefix)).sorted().toList();
    }
}
