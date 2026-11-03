# Architecture

The stable core is split from Paper integration:

```text
Paper lifecycle/events/commands
            |
            v
SQLite repository + UUID preferences
            |
            v
RadarPolicy (PRIVATE -> TEAM -> PUBLIC authorization)
            |
            v
RadarCompatibilityProvider
            |
            v
Filtered action-bar renderer
```

`AllyRepository` owns all mutations and uses synchronized prepared-statement operations. The plugin caches player preferences and team summaries in memory, invalidating them after membership or preference changes. The radar scheduler runs on the server thread, copies only online player state, applies authorization, and renders names, relative directions, and rounded distances without querying SQLite on every refresh.

Version-sensitive behavior belongs behind `RadarCompatibilityProvider`. Native locator-bar filtering is intentionally not implemented because Paper does not expose stable per-recipient filtering.
