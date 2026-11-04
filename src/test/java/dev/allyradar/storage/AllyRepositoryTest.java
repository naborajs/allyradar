package dev.allyradar.storage;

import dev.allyradar.model.PlayerPreferences;
import dev.allyradar.model.TeamMember;
import dev.allyradar.model.VisibilityMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllyRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void teamMembershipAndPreferencesPersistInSqlite() {
        final File database = temporaryDirectory.resolve("allyradar.db").toFile();
        final UUID owner = UUID.randomUUID();
        final UUID member = UUID.randomUUID();
        try (AllyRepository repository = new AllyRepository(database, VisibilityMode.TEAM, true)) {
            repository.createTeam(owner, "Explorers", 10);
            repository.invite(owner, member, "Member", Duration.ofHours(1), 10);
            repository.accept(member, "Explorers", "Member", 10);
            assertTrue(repository.sameTeam(owner, member));
            repository.setVisibility(member, VisibilityMode.PRIVATE);
            final PlayerPreferences preferences = repository.preferences(member);
            assertEquals(VisibilityMode.PRIVATE, preferences.visibility());
        }
        try (AllyRepository reopened = new AllyRepository(database, VisibilityMode.PUBLIC, false)) {
            assertTrue(reopened.teamOf(member).isPresent());
            assertEquals(VisibilityMode.PRIVATE, reopened.preferences(member).visibility());
            assertFalse(reopened.preferences(UUID.randomUUID()).radarEnabled());
        }
    }

    @Test
    void expiredInvitationCannotBeAccepted() {
        final File database = temporaryDirectory.resolve("expired.db").toFile();
        final UUID owner = UUID.randomUUID();
        final UUID invitee = UUID.randomUUID();
        try (AllyRepository repository = new AllyRepository(database, VisibilityMode.TEAM, true)) {
            repository.createTeam(owner, "Explorers", 10);
            repository.invite(owner, invitee, "Invitee", Duration.ofMillis(-1), 10);
            assertThrows(IllegalArgumentException.class,
                    () -> repository.accept(invitee, "Explorers", "Invitee", 10));
            assertFalse(repository.teamOf(invitee).isPresent());
        }
    }

    @Test
    void createTeamWithCustomOwnerNameStoresAndReturnsOwnerName() {
        final File database = temporaryDirectory.resolve("owner_name.db").toFile();
        final UUID owner = UUID.randomUUID();
        try (AllyRepository repository = new AllyRepository(database, VisibilityMode.TEAM, true)) {
            repository.createTeam(owner, "CaptainSteve", "Navigators", 5);
            final List<TeamMember> members = repository.members(owner);
            assertEquals(1, members.size());
            assertEquals("CaptainSteve", members.getFirst().lastKnownName());
            assertEquals(owner, members.getFirst().playerId());
            assertTrue(members.getFirst().owner());
        }
    }

    @Test
    void createTeamDefaultOverloadUsesOwnerLiteral() {
        final File database = temporaryDirectory.resolve("default_owner.db").toFile();
        final UUID owner = UUID.randomUUID();
        try (AllyRepository repository = new AllyRepository(database, VisibilityMode.TEAM, true)) {
            repository.createTeam(owner, "Navigators", 5);
            final List<TeamMember> members = repository.members(owner);
            assertEquals(1, members.size());
            assertEquals("Owner", members.getFirst().lastKnownName());
        }
    }

    @Test
    void createTeamNullOrBlankOwnerNameFallsBackToOwnerLiteral() {
        final File database = temporaryDirectory.resolve("fallback_owner.db").toFile();
        final UUID owner1 = UUID.randomUUID();
        final UUID owner2 = UUID.randomUUID();
        try (AllyRepository repository = new AllyRepository(database, VisibilityMode.TEAM, true)) {
            repository.createTeam(owner1, null, "NavigatorsOne", 5);
            assertEquals("Owner", repository.members(owner1).getFirst().lastKnownName());

            repository.createTeam(owner2, "   ", "NavigatorsTwo", 5);
            assertEquals("Owner", repository.members(owner2).getFirst().lastKnownName());
        }
    }

    @Test
    void acceptWithoutTeamNameResolvesSinglePendingInvitation() {
        final File database = temporaryDirectory.resolve("accept_no_arg.db").toFile();
        final UUID owner = UUID.randomUUID();
        final UUID invitee = UUID.randomUUID();
        try (AllyRepository repository = new AllyRepository(database, VisibilityMode.TEAM, true)) {
            repository.createTeam(owner, "Captain", "Starfleet", 5);
            repository.invite(owner, invitee, "Crewman", Duration.ofHours(2), 5);

            assertEquals(List.of("Starfleet"), repository.pendingInvitations(invitee, "Crewman"));

            // Pass null or blank string to accept without specifying team name
            repository.accept(invitee, "", "Crewman", 5);
            assertTrue(repository.sameTeam(owner, invitee));
            assertTrue(repository.pendingInvitations(invitee, "Crewman").isEmpty());
        }
    }

    @Test
    void acceptByNameMatchingResolvesGeyserBedrockPlayer() {
        final File database = temporaryDirectory.resolve("geyser_player.db").toFile();
        final UUID owner = UUID.randomUUID();
        final UUID offlineInviteUuid = UUID.randomUUID();
        final UUID actualRuntimeBedrockUuid = UUID.randomUUID(); // Different UUID assigned by Floodgate/Geyser
        try (AllyRepository repository = new AllyRepository(database, VisibilityMode.TEAM, true)) {
            repository.createTeam(owner, "Captain", "BedrockCrew", 5);
            // Invited using offline UUID and name with Bedrock prefix
            repository.invite(owner, offlineInviteUuid, ".Aloneboy18k", Duration.ofHours(2), 5);

            // Bedrock player joins with runtime Floodgate UUID and accepts
            assertEquals(List.of("BedrockCrew"), repository.pendingInvitations(actualRuntimeBedrockUuid, ".Aloneboy18k"));
            repository.accept(actualRuntimeBedrockUuid, "", ".Aloneboy18k", 5);

            assertTrue(repository.sameTeam(owner, actualRuntimeBedrockUuid));
            assertEquals(".Aloneboy18k", repository.members(actualRuntimeBedrockUuid).getFirst().lastKnownName());
        }
    }

    @Test
    void acceptWithNoPendingInvitationsThrows() {
        final File database = temporaryDirectory.resolve("no_invites.db").toFile();
        final UUID player = UUID.randomUUID();
        try (AllyRepository repository = new AllyRepository(database, VisibilityMode.TEAM, true)) {
            assertThrows(IllegalArgumentException.class, () -> repository.accept(player, "", "Player", 5));
        }
    }

    @Test
    void denyWithoutTeamNameDeclinesInvitation() {
        final File database = temporaryDirectory.resolve("deny_invitation.db").toFile();
        final UUID owner = UUID.randomUUID();
        final UUID invitee = UUID.randomUUID();
        try (AllyRepository repository = new AllyRepository(database, VisibilityMode.TEAM, true)) {
            repository.createTeam(owner, "Captain", "Fleet", 5);
            repository.invite(owner, invitee, "Sailor", Duration.ofHours(2), 5);

            repository.deny(invitee, "", "Sailor");
            assertTrue(repository.pendingInvitations(invitee, "Sailor").isEmpty());
        }
    }

    @Test
    void soleOwnerCallingLeaveDisbandsTeam() {
        final File database = temporaryDirectory.resolve("sole_owner_leave.db").toFile();
        final UUID owner = UUID.randomUUID();
        try (AllyRepository repository = new AllyRepository(database, VisibilityMode.TEAM, true)) {
            repository.createTeam(owner, "Alice", "SoloCrew", 5);
            assertTrue(repository.teamOf(owner).isPresent());

            // Owner is the only member and calls leave -> should auto-disband
            repository.leave(owner);
            assertTrue(repository.teamOf(owner).isEmpty(), "Leaving as the sole member must disband the team");
        }
    }

    @Test
    void ownerWithMembersCallingLeaveThrowsGuidance() {
        final File database = temporaryDirectory.resolve("owner_with_members.db").toFile();
        final UUID owner = UUID.randomUUID();
        final UUID member = UUID.randomUUID();
        try (AllyRepository repository = new AllyRepository(database, VisibilityMode.TEAM, true)) {
            repository.createTeam(owner, "Alice", "DuoCrew", 5);
            repository.invite(owner, member, "Bob", Duration.ofHours(1), 5);
            repository.accept(member, "", "Bob", 5);

            // Owner tries to leave while another member is in the team
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> repository.leave(owner));
            assertTrue(ex.getMessage().contains("disband"));
            assertTrue(repository.teamOf(owner).isPresent());
        }
    }

    @Test
    void hudAndCustomRadiusPreferencesPersistInSqlite() {
        final File database = temporaryDirectory.resolve("hud_radius.db").toFile();
        final UUID player = UUID.randomUUID();
        try (AllyRepository repository = new AllyRepository(database, VisibilityMode.TEAM, true)) {
            final PlayerPreferences defaults = repository.preferences(player);
            assertTrue(defaults.hudEnabled(), "HUD should be enabled by default");
            assertEquals(0.0, defaults.customRadius(), "Custom radius should be 0.0 by default");

            repository.setHudEnabled(player, false);
            repository.setCustomRadius(player, 125.0);

            final PlayerPreferences updated = repository.preferences(player);
            assertFalse(updated.hudEnabled());
            assertEquals(125.0, updated.customRadius());
        }
        try (AllyRepository reopened = new AllyRepository(database, VisibilityMode.TEAM, true)) {
            final PlayerPreferences persisted = reopened.preferences(player);
            assertFalse(persisted.hudEnabled());
            assertEquals(125.0, persisted.customRadius());
        }
    }
}
