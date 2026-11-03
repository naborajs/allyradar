# AllyRadar

AllyRadar is a lightweight Paper plugin for private team management and consent-based teammate tracking.

## Current release

- Version: `1.0.0`
- Target: Paper `26.3.build.238-stable`
- Java: 25+
- Artifact: `build/libs/AllyRadar-1.0.0.jar`
- Radar renderer: filtered action bar

## Features

- Persistent UUID-backed teams with one team per player.
- Team creation, invitations, acceptance, denial, leaving, kicking, ownership transfer, disbanding, member listing, and team radar settings.
- Invitation expiration and configurable team size.
- Independent location sharing: `PRIVATE`, `TEAM`, or `PUBLIC`.
- Independent radar receiving preference.
- Server-controlled public radar opt-in.
- SQLite persistence with prepared statements and a migration table.
- Privacy filtering before any radar output is created.

## Install

1. Run a Paper 26.3 server with Java 25 or newer.
2. Stop the server.
3. Copy `build/libs/AllyRadar-1.0.0.jar` into the server's `plugins/` directory.
4. Start the server and review `plugins/AllyRadar/config.yml`.

Do not copy a development database or install this artifact on a production server without testing it in an isolated server first.

## Commands

```text
/team create <name>
/team invite <player>
/team accept <team>
/team deny <team>
/team leave
/team kick <player>
/team transfer <player>
/team info
/team list
/team settings radar <on|off>
/team disband

/radar on|off
/radar visibility <private|team|public>
/radar status
/radar who
```

`/allyradar` shows help and `/allyradar reload` reloads configuration values that do not require a restart.

## Privacy model

The tracked player's setting controls disclosure. `PRIVATE` always blocks output, including to team owners. `TEAM` requires the receiver and target to share a team and the team radar setting to be enabled. `PUBLIC` requires `public-radar-enabled: true`. Receivers can disable their own radar. The action bar exposes names, relative direction, and rounded distance only; it never displays exact coordinates.

## Locator-bar limitation

Paper's stable API does not provide per-recipient filtering for the native locator bar. AllyRadar therefore does not claim private native locator-bar support. The current action-bar provider is filtered before rendering. A future packet adapter must be pinned and tested for the exact server/client version before use.

## Development

```bash
./gradlew clean test build
```

See `AGENTS.md`, `docs/architecture.md`, `docs/configuration.md`, and `docs/compatibility.md`.
