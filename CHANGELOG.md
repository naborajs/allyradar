# Changelog

## 1.0.0 - Unreleased

### Added

- UUID-backed persistent teams and SQLite migrations.
- Expiring invitations, membership management, ownership transfer, and disbanding.
- PRIVATE, TEAM, and PUBLIC location-sharing modes.
- Receiver radar preferences and owner-controlled team radar.
- Filtered action-bar radar with direction and rounded distance.
- Paper 26.3 build, automated unit tests, documentation, and CI workflow.

### Known limitations

- Native locator-bar per-recipient filtering is not exposed by stable Paper APIs and is not enabled.
- A live Paper server integration test has not been run in this development container.
