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
}
