package dev.allyradar.storage;

import dev.allyradar.model.PlayerPreferences;
import dev.allyradar.model.TeamMember;
import dev.allyradar.model.TeamSummary;
import dev.allyradar.model.VisibilityMode;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class AllyRepository implements AutoCloseable {
    private final Connection connection;
    private final VisibilityMode defaultVisibility;
    private final boolean defaultRadarEnabled;
    private final boolean defaultTeamRadarEnabled;

    public AllyRepository(final JavaPlugin plugin, final String databaseFile,
                          final VisibilityMode defaultVisibility, final boolean defaultRadarEnabled) {
        this(plugin, databaseFile, defaultVisibility, defaultRadarEnabled, true);
    }

    public AllyRepository(final JavaPlugin plugin, final String databaseFile,
                          final VisibilityMode defaultVisibility, final boolean defaultRadarEnabled,
                          final boolean defaultTeamRadarEnabled) {
        this(new File(plugin.getDataFolder(), databaseFile), defaultVisibility, defaultRadarEnabled,
                defaultTeamRadarEnabled);
    }

    public AllyRepository(final File file, final VisibilityMode defaultVisibility, final boolean defaultRadarEnabled) {
        this(file, defaultVisibility, defaultRadarEnabled, true);
    }

    public AllyRepository(final File file, final VisibilityMode defaultVisibility, final boolean defaultRadarEnabled,
                          final boolean defaultTeamRadarEnabled) {
        this.defaultVisibility = defaultVisibility;
        this.defaultRadarEnabled = defaultRadarEnabled;
        this.defaultTeamRadarEnabled = defaultTeamRadarEnabled;
        try {
            final File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("Could not create the plugin data directory.");
            }
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA busy_timeout = 5000");
            }
            migrate();
        } catch (SQLException exception) {
            throw failure("Could not open AllyRadar database", exception);
        }
    }

    private void migrate() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS schema_migrations (
                        version INTEGER PRIMARY KEY,
                        applied_at INTEGER NOT NULL
                    )
                    """);
            if (!migrationApplied(statement, 1)) {
                connection.setAutoCommit(false);
                try {
                    statement.executeUpdate(String.format("""
                            CREATE TABLE teams (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                name TEXT NOT NULL COLLATE NOCASE UNIQUE,
                                owner_uuid TEXT NOT NULL,
                                radar_enabled INTEGER NOT NULL DEFAULT %d,
                                created_at INTEGER NOT NULL
                            )
                            """, defaultTeamRadarEnabled ? 1 : 0));
                    statement.executeUpdate("""
                            CREATE TABLE team_members (
                                team_id INTEGER NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
                                player_uuid TEXT PRIMARY KEY,
                                last_known_name TEXT NOT NULL,
                                joined_at INTEGER NOT NULL
                            )
                            """);
                    statement.executeUpdate("""
                            CREATE TABLE invitations (
                                team_id INTEGER NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
                                player_uuid TEXT NOT NULL,
                                player_name TEXT NOT NULL,
                                expires_at INTEGER NOT NULL,
                                PRIMARY KEY (team_id, player_uuid)
                            )
                            """);
                    statement.executeUpdate("""
                            CREATE TABLE player_preferences (
                                player_uuid TEXT PRIMARY KEY,
                                visibility TEXT NOT NULL,
                                radar_enabled INTEGER NOT NULL
                            )
                            """);
                    statement.executeUpdate("CREATE INDEX members_by_team ON team_members(team_id)");
                    statement.executeUpdate("CREATE INDEX invitations_by_player ON invitations(player_uuid)");
                    try (PreparedStatement migration = connection.prepareStatement(
                            "INSERT INTO schema_migrations(version, applied_at) VALUES(?, ?)")) {
                        migration.setInt(1, 1);
                        migration.setLong(2, Instant.now().toEpochMilli());
                        migration.executeUpdate();
                    }
                    connection.commit();
                } catch (SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        }
    }

    private boolean migrationApplied(final Statement statement, final int version) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT 1 FROM schema_migrations WHERE version = ?")) {
            query.setInt(1, version);
            try (ResultSet result = query.executeQuery()) {
                return result.next();
            }
        }
    }

    public synchronized PlayerPreferences preferences(final UUID playerId) {
        try {
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO player_preferences(player_uuid, visibility, radar_enabled)
                    VALUES(?, ?, ?)
                    ON CONFLICT(player_uuid) DO NOTHING
                    """)) {
                insert.setString(1, playerId.toString());
                insert.setString(2, defaultVisibility.name());
                insert.setBoolean(3, defaultRadarEnabled);
                insert.executeUpdate();
            }
            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT visibility, radar_enabled FROM player_preferences WHERE player_uuid = ?")) {
                query.setString(1, playerId.toString());
                try (ResultSet result = query.executeQuery()) {
                    result.next();
                    return new PlayerPreferences(VisibilityMode.parse(result.getString(1)), result.getBoolean(2));
                }
            }
        } catch (SQLException exception) {
            throw failure("Could not load player preferences", exception);
        }
    }

    public synchronized void setVisibility(final UUID playerId, final VisibilityMode visibility) {
        updatePreference(playerId, "visibility", visibility.name());
    }

    public synchronized void setRadarEnabled(final UUID playerId, final boolean enabled) {
        try {
            preferences(playerId);
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE player_preferences SET radar_enabled = ? WHERE player_uuid = ?")) {
                update.setBoolean(1, enabled);
                update.setString(2, playerId.toString());
                update.executeUpdate();
            }
        } catch (SQLException exception) {
            throw failure("Could not update radar preference", exception);
        }
    }

    private void updatePreference(final UUID playerId, final String column, final String value) {
        if (!column.equals("visibility")) {
            throw new IllegalArgumentException("Unsupported preference.");
        }
        try {
            preferences(playerId);
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE player_preferences SET visibility = ? WHERE player_uuid = ?")) {
                update.setString(1, value);
                update.setString(2, playerId.toString());
                update.executeUpdate();
            }
        } catch (SQLException exception) {
            throw failure("Could not update location visibility", exception);
        }
    }

    public synchronized Optional<TeamSummary> teamOf(final UUID playerId) {
        try (PreparedStatement query = connection.prepareStatement("""
                SELECT t.id, t.name, t.owner_uuid, t.radar_enabled,
                       (SELECT COUNT(*) FROM team_members m2 WHERE m2.team_id = t.id)
                FROM teams t JOIN team_members m ON m.team_id = t.id
                WHERE m.player_uuid = ?
                """)) {
            query.setString(1, playerId.toString());
            try (ResultSet result = query.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(summary(result));
            }
        } catch (SQLException exception) {
            throw failure("Could not load team", exception);
        }
    }

    public synchronized boolean sameTeam(final UUID first, final UUID second) {
        try (PreparedStatement query = connection.prepareStatement("""
                SELECT 1 FROM team_members a JOIN team_members b ON a.team_id = b.team_id
                WHERE a.player_uuid = ? AND b.player_uuid = ?
                """)) {
            query.setString(1, first.toString());
            query.setString(2, second.toString());
            try (ResultSet result = query.executeQuery()) {
                return result.next();
            }
        } catch (SQLException exception) {
            throw failure("Could not compare team membership", exception);
        }
    }

    public synchronized TeamSummary createTeam(final UUID owner, final String name, final int maximumSize) {
        return createTeam(owner, "Owner", name, maximumSize);
    }

    public synchronized TeamSummary createTeam(final UUID owner, final String ownerName, final String name, final int maximumSize) {
        final String resolvedOwnerName = (ownerName == null || ownerName.isBlank()) ? "Owner" : ownerName.trim();
        final String normalized = name.trim();
        if (!normalized.matches("[A-Za-z0-9][A-Za-z0-9 _-]{1,23}")) {
            throw new IllegalArgumentException("Team names must be 2-24 characters and use letters, numbers, spaces, _ or -.");
        }
        if (teamOf(owner).isPresent()) {
            throw new IllegalArgumentException("You are already in a team.");
        }
        try {
            connection.setAutoCommit(false);
            final long teamId;
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO teams(name, owner_uuid, created_at) VALUES(?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                insert.setString(1, normalized);
                insert.setString(2, owner.toString());
                insert.setLong(3, Instant.now().toEpochMilli());
                insert.executeUpdate();
                try (ResultSet keys = insert.getGeneratedKeys()) {
                    keys.next();
                    teamId = keys.getLong(1);
                }
            }
            addMember(teamId, owner, resolvedOwnerName);
            connection.commit();
            return teamOf(owner).orElseThrow();
        } catch (SQLException exception) {
            rollbackQuietly();
            if (exception.getMessage() != null && exception.getMessage().contains("UNIQUE")) {
                throw new IllegalArgumentException("A team with that name already exists.");
            }
            throw failure("Could not create team", exception);
        } finally {
            autoCommitQuietly();
        }
    }

    public synchronized void invite(final UUID owner, final UUID target, final String targetName,
                                    final Duration expiration, final int maximumSize) {
        final TeamSummary team = ownerTeam(owner);
        if (team.memberCount() >= maximumSize) {
            throw new IllegalArgumentException("The team is full.");
        }
        if (teamOf(target).isPresent()) {
            throw new IllegalArgumentException("That player is already in a team.");
        }
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT expires_at FROM invitations WHERE team_id = ? AND player_uuid = ?")) {
            query.setLong(1, team.id());
            query.setString(2, target.toString());
            try (ResultSet result = query.executeQuery()) {
                if (result.next() && result.getLong(1) > Instant.now().toEpochMilli()) {
                    throw new IllegalArgumentException("That player already has a pending invitation.");
                }
            }
        } catch (SQLException exception) {
            throw failure("Could not inspect invitation", exception);
        }
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO invitations(team_id, player_uuid, player_name, expires_at)
                VALUES(?, ?, ?, ?)
                ON CONFLICT(team_id, player_uuid) DO UPDATE SET
                    player_name = excluded.player_name, expires_at = excluded.expires_at
                """)) {
            insert.setLong(1, team.id());
            insert.setString(2, target.toString());
            insert.setString(3, targetName);
            insert.setLong(4, Instant.now().plus(expiration).toEpochMilli());
            insert.executeUpdate();
        } catch (SQLException exception) {
            throw failure("Could not create invitation", exception);
        }
    }

    public synchronized List<String> pendingInvitations(final UUID player, final String playerName) {
        final List<String> teams = new ArrayList<>();
        try (PreparedStatement query = connection.prepareStatement("""
                SELECT t.name FROM invitations i JOIN teams t ON t.id = i.team_id
                WHERE (i.player_uuid = ? OR (i.player_name != '' AND LOWER(i.player_name) = LOWER(?)))
                  AND i.expires_at > ?
                ORDER BY i.expires_at DESC
                """)) {
            query.setString(1, player != null ? player.toString() : "");
            query.setString(2, playerName != null ? playerName : "");
            query.setLong(3, Instant.now().toEpochMilli());
            try (ResultSet result = query.executeQuery()) {
                while (result.next()) {
                    teams.add(result.getString(1));
                }
            }
        } catch (SQLException exception) {
            throw failure("Could not load invitations", exception);
        }
        return teams;
    }

    public synchronized TeamSummary accept(final UUID player, final String teamName, final String playerName,
                                            final int maximumSize) {
        if (teamOf(player).isPresent()) {
            throw new IllegalArgumentException("You are already in a team.");
        }
        final String targetTeamName;
        if (teamName == null || teamName.isBlank()) {
            final List<String> pending = pendingInvitations(player, playerName);
            if (pending.isEmpty()) {
                throw new IllegalArgumentException("You have no pending team invitations.");
            }
            if (pending.size() > 1) {
                throw new IllegalArgumentException("You have invitations from multiple teams: "
                        + String.join(", ", pending) + ". Use /team accept <teamName>.");
            }
            targetTeamName = pending.getFirst();
        } else {
            targetTeamName = teamName.trim();
        }
        try {
            connection.setAutoCommit(false);
            final long teamId;
            try (PreparedStatement query = connection.prepareStatement("""
                    SELECT i.team_id FROM invitations i JOIN teams t ON t.id = i.team_id
                    WHERE (i.player_uuid = ? OR (i.player_name != '' AND LOWER(i.player_name) = LOWER(?)))
                      AND LOWER(t.name) = LOWER(?) AND i.expires_at > ?
                    """)) {
                query.setString(1, player.toString());
                query.setString(2, playerName != null ? playerName : "");
                query.setString(3, targetTeamName);
                query.setLong(4, Instant.now().toEpochMilli());
                try (ResultSet result = query.executeQuery()) {
                    if (!result.next()) {
                        throw new IllegalArgumentException("No active invitation was found for that team.");
                    }
                    teamId = result.getLong(1);
                }
            }
            if (countMembers(teamId) >= maximumSize) {
                throw new IllegalArgumentException("The team is full.");
            }
            addMember(teamId, player, playerName);
            try (PreparedStatement delete = connection.prepareStatement("""
                    DELETE FROM invitations WHERE team_id = ?
                    AND (player_uuid = ? OR (player_name != '' AND LOWER(player_name) = LOWER(?)))
                    """)) {
                delete.setLong(1, teamId);
                delete.setString(2, player.toString());
                delete.setString(3, playerName != null ? playerName : "");
                delete.executeUpdate();
            }
            connection.commit();
            return teamOf(player).orElseThrow();
        } catch (SQLException exception) {
            rollbackQuietly();
            throw failure("Could not accept invitation", exception);
        } finally {
            autoCommitQuietly();
        }
    }

    public synchronized void deny(final UUID player, final String teamName) {
        deny(player, teamName, "");
    }

    public synchronized void deny(final UUID player, final String teamName, final String playerName) {
        final String targetTeamName;
        if (teamName == null || teamName.isBlank()) {
            final List<String> pending = pendingInvitations(player, playerName);
            if (pending.isEmpty()) {
                throw new IllegalArgumentException("You have no pending team invitations.");
            }
            if (pending.size() > 1) {
                throw new IllegalArgumentException("You have invitations from multiple teams: "
                        + String.join(", ", pending) + ". Use /team deny <teamName>.");
            }
            targetTeamName = pending.getFirst();
        } else {
            targetTeamName = teamName.trim();
        }
        try (PreparedStatement delete = connection.prepareStatement("""
                DELETE FROM invitations
                WHERE (player_uuid = ? OR (player_name != '' AND LOWER(player_name) = LOWER(?)))
                AND team_id = (SELECT id FROM teams WHERE LOWER(name) = LOWER(?))
                """)) {
            delete.setString(1, player.toString());
            delete.setString(2, playerName != null ? playerName : "");
            delete.setString(3, targetTeamName);
            delete.executeUpdate();
        } catch (SQLException exception) {
            throw failure("Could not deny invitation", exception);
        }
    }

    public synchronized void leave(final UUID player) {
        final TeamSummary team = teamOf(player).orElseThrow(() -> new IllegalArgumentException("You are not in a team."));
        if (team.owner().equals(player)) {
            throw new IllegalArgumentException("Transfer ownership or disband the team before leaving.");
        }
        removeMember(player);
    }

    public synchronized void kick(final UUID owner, final UUID target) {
        final TeamSummary team = ownerTeam(owner);
        if (team.owner().equals(target)) {
            throw new IllegalArgumentException("The owner cannot be kicked.");
        }
        if (!sameTeam(owner, target)) {
            throw new IllegalArgumentException("That player is not on your team.");
        }
        removeMember(target);
    }

    public synchronized void transfer(final UUID owner, final UUID target) {
        final TeamSummary team = ownerTeam(owner);
        if (!sameTeam(owner, target)) {
            throw new IllegalArgumentException("The target must be an existing team member.");
        }
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE teams SET owner_uuid = ? WHERE id = ?")) {
            update.setString(1, target.toString());
            update.setLong(2, team.id());
            update.executeUpdate();
        } catch (SQLException exception) {
            throw failure("Could not transfer ownership", exception);
        }
    }

    public synchronized void disband(final UUID owner) {
        final TeamSummary team = ownerTeam(owner);
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM teams WHERE id = ?")) {
            delete.setLong(1, team.id());
            delete.executeUpdate();
        } catch (SQLException exception) {
            throw failure("Could not disband team", exception);
        }
    }

    public synchronized void setTeamRadar(final UUID owner, final boolean enabled) {
        final TeamSummary team = ownerTeam(owner);
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE teams SET radar_enabled = ? WHERE id = ?")) {
            update.setBoolean(1, enabled);
            update.setLong(2, team.id());
            update.executeUpdate();
        } catch (SQLException exception) {
            throw failure("Could not update team radar", exception);
        }
    }

    public synchronized List<TeamMember> members(final UUID player) {
        final TeamSummary team = teamOf(player).orElseThrow(() -> new IllegalArgumentException("You are not in a team."));
        final List<TeamMember> members = new ArrayList<>();
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT player_uuid, last_known_name FROM team_members WHERE team_id = ? ORDER BY last_known_name")) {
            query.setLong(1, team.id());
            try (ResultSet result = query.executeQuery()) {
                while (result.next()) {
                    final UUID memberId = UUID.fromString(result.getString(1));
                    members.add(new TeamMember(memberId, result.getString(2), memberId.equals(team.owner())));
                }
            }
            return members;
        } catch (SQLException exception) {
            throw failure("Could not list team members", exception);
        }
    }

    private TeamSummary ownerTeam(final UUID owner) {
        final TeamSummary team = teamOf(owner).orElseThrow(() -> new IllegalArgumentException("You are not in a team."));
        if (!team.owner().equals(owner)) {
            throw new IllegalArgumentException("Only the team owner can perform that action.");
        }
        return team;
    }

    private TeamSummary summary(final ResultSet result) throws SQLException {
        return new TeamSummary(result.getLong(1), result.getString(2), UUID.fromString(result.getString(3)),
                result.getInt(5), result.getBoolean(4));
    }

    private int countMembers(final long teamId) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT COUNT(*) FROM team_members WHERE team_id = ?")) {
            query.setLong(1, teamId);
            try (ResultSet result = query.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private void addMember(final long teamId, final UUID player, final String name) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO team_members(team_id, player_uuid, last_known_name, joined_at) VALUES(?, ?, ?, ?)")) {
            insert.setLong(1, teamId);
            insert.setString(2, player.toString());
            insert.setString(3, name);
            insert.setLong(4, Instant.now().toEpochMilli());
            insert.executeUpdate();
        }
    }

    private void removeMember(final UUID player) {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM team_members WHERE player_uuid = ?")) {
            delete.setString(1, player.toString());
            if (delete.executeUpdate() == 0) {
                throw new IllegalArgumentException("You are not in a team.");
            }
        } catch (SQLException exception) {
            throw failure("Could not update team membership", exception);
        }
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Preserve the original failure.
        }
    }

    private void autoCommitQuietly() {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException ignored) {
            // The connection will be closed during plugin shutdown.
        }
    }

    private IllegalStateException failure(final String message, final SQLException exception) {
        return new IllegalStateException(message, exception);
    }

    @Override
    public synchronized void close() {
        try {
            connection.close();
        } catch (SQLException exception) {
            throw failure("Could not close AllyRadar database", exception);
        }
    }
}
