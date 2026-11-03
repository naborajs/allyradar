package dev.allyradar.model;

import java.util.UUID;

public record TeamSummary(long id, String name, UUID owner, int memberCount, boolean radarEnabled) {
}
