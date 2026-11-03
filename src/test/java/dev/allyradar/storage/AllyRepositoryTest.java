package dev.allyradar.storage;

import dev.allyradar.model.PlayerPreferences;
import dev.allyradar.model.VisibilityMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
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
}
