package dev.allyradar.model;

import java.util.UUID;

public record TeamMember(UUID playerId, String lastKnownName, boolean owner) {
}
