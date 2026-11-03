package dev.allyradar.model;

import java.util.UUID;

public record TrackedPlayer(UUID playerId, String name, double distance, double bearing, String worldName) {
}
