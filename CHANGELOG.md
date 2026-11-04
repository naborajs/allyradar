# Changelog

## 1.0.0 - Unreleased

### Added

- UUID-backed persistent teams and SQLite migrations.
- Expiring invitations, membership management, ownership transfer, and disbanding.
- PRIVATE, TEAM, and PUBLIC location-sharing modes with centralized fail-closed authorization.
- Effective visibility resolution where TEAM visibility without a valid team behaves as PRIVATE.
- Receiver radar preferences and owner-controlled team radar.
- Filtered action-bar radar with direction and rounded distance.
- Adventure BossBar radar display mode (`BOSS_BAR`, `BOTH`, `ACTION_BAR`, `DISABLED`) to prevent HUD conflicts with other plugins.
- Mathematical bearing correction for Minecraft clockwise yaw space (`atan2(-dx, dz)`).
- Immediate cache invalidation and HUD refresh pipeline on commands and player lifecycle events.
- Stateful HUD clearing to eliminate persistent "no authorized teammates nearby" action-bar messages.
- Paper 26.3 build, automated unit tests, documentation, and CI workflow.
- Open-source project governance: MIT license, contribution guide, code of conduct, issue forms, Dependabot, dependency review, and tagged-release automation.
- Thread-safe `ConcurrentHashMap` preference and team caches.

### Known limitations

- Native locator-bar per-recipient filtering is not exposed by stable Paper APIs and is not enabled.
- A live Paper server integration test has not been run in this development container.
