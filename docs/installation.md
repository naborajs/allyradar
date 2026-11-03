# Installation and first-run guide

## Requirements

- Paper `26.3.build.238-stable` or the verified compatible build listed in `docs/compatibility.md`.
- Java 25 or newer.
- A separate test server for first installation.

## Install

1. Build with `./gradlew clean test build`, or download the release JAR.
2. Stop the isolated Paper server.
3. Copy `build/libs/AllyRadar-1.0.0.jar` into `<server>/plugins/`.
4. Start the server once.
5. Inspect `<server>/plugins/AllyRadar/config.yml`.
6. Stop the server before changing startup-only settings, then start again.

The plugin creates `allyradar.db` in its data folder. Back up that file while the server is stopped. Never edit the SQLite file while the server is running.

## First-run test

Use two test accounts, Alice and Bob:

1. Alice runs `/team create Explorers`.
2. Alice runs `/team invite Bob`.
3. Bob runs `/team accept Explorers`.
4. Confirm both players with `/team list`.
5. Confirm the default `TEAM` sharing with `/radar status`.
6. Toggle Bob's setting to `PRIVATE` and confirm Alice no longer sees Bob in `/radar who`.
7. Toggle team radar off and confirm all team radar output disappears.
8. Test `/radar off` for the receiver.

## Upgrade and rollback

Stop the server before replacing the JAR. Keep a copy of the previous JAR and `allyradar.db`. Review the release notes and compatibility matrix before upgrading. Do not roll back across an untested database migration without restoring a backup.
