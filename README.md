# AllyRadar

[![CI](https://github.com/naborajs/allyradar/actions/workflows/ci.yml/badge.svg)](https://github.com/naborajs/allyradar/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

AllyRadar is an open-source Paper plugin for persistent teams and consent-based teammate radar. It is designed around one rule: a player controls whether their location can be shared, and privacy is enforced before any radar output is created.

**AI assistant and server operator quick start:** read [`AGENT_GUIDE.md`](AGENT_GUIDE.md). It is the operational guide for finding the right files, building the JAR, installing it on an isolated Paper server, controlling it, testing privacy, backing it up, upgrading it, and reporting verified status.

> **Status:** version `1.0.0` is a release candidate for isolated server testing. The project compiles and automated tests pass; live Paper loading and multiplayer smoke testing still need to be completed by a server operator.

## Highlights

- UUID-backed teams with one team per player.
- Invitations with expiration, acceptance, denial, and team-size limits.
- Ownership transfer, member removal, leaving, disbanding, and team settings.
- Three independent sharing modes: `PRIVATE`, `TEAM`, and `PUBLIC`.
- Receiver-controlled radar on/off preference.
- Owner-controlled team radar on/off setting.
- Filtered action-bar radar showing names, relative direction, and rounded distance.
- Same-dimension filtering and configurable maximum range.
- SQLite persistence with prepared statements, foreign keys, indexes, and migration tracking.
- In-memory preference/team cache to avoid SQLite queries during every radar refresh.
- GitHub Actions CI, release packaging, dependency review, Dependabot, issue forms, and contribution guidance.

## Compatibility

| Component | Supported target |
|---|---|
| Minecraft/Paper | Paper `26.3.build.238-stable` |
| Java | 25 or newer |
| Build | Gradle 9.8.2 via the included wrapper |
| Radar | Filtered action bar |
| Native locator bar | Not claimed; per-recipient filtering is not exposed by stable Paper APIs |

See [`docs/compatibility.md`](docs/compatibility.md) for verified versus unverified status and the upgrade procedure.

## Install for testing

1. Install Java 25+.
2. Run an isolated Paper 26.3 test server.
3. Download `AllyRadar-1.0.0.jar` from the GitHub release or build it locally.
4. Stop the test server and copy the JAR into its `plugins/` directory.
5. Start the server and review `plugins/AllyRadar/config.yml`.
6. Test with at least two accounts before considering deployment.

```bash
./gradlew clean test build
```

The local artifact is `build/libs/AllyRadar-1.0.0.jar`. Never copy a development database, server world, or private configuration into this repository. Do not install this release on a production server without an isolated verification pass and explicit approval.

For a copy-and-run operator procedure with shell variables, console commands, player commands, backup steps, rollback steps, and an acceptance checklist, use [`AGENT_GUIDE.md`](AGENT_GUIDE.md).

## Commands

### Team management

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
```

### Radar and privacy

```text
/radar on
/radar off
/radar visibility private
/radar visibility team
/radar visibility public
/radar status
/radar who
```

### Administration

```text
/allyradar
/allyradar reload
```

`/allyradar reload` requires `allyradar.admin`. Some startup-only values, including the database path and scheduler interval, require a restart.

Full argument behavior is documented in [`docs/commands.md`](docs/commands.md).

## Privacy model

The tracked player's setting controls disclosure:

| Target setting | Who may receive radar output |
|---|---|
| `PRIVATE` | Nobody |
| `TEAM` | Same-team receivers while team radar is enabled |
| `PUBLIC` | Any eligible receiver only when the server enables public radar |

The receiver can disable their own radar. Offline players and players in another dimension are never included in the action-bar radar. Team owners cannot override `PRIVATE`. AllyRadar does not display exact coordinates and does not log player locations.

### Native locator-bar limitation

Paper's stable API does not provide per-recipient native locator-bar filtering. AllyRadar therefore does not pretend to provide private vanilla locator-bar markers. The current implementation uses a filtered action bar. A future packet adapter must be version-pinned, independently tested, and privacy-reviewed before activation.

## Configuration

The generated configuration is documented in [`docs/configuration.md`](docs/configuration.md). Important defaults include:

```yaml
max-team-size: 10
invitation-expiration-hours: 48
default-location-visibility: TEAM
default-radar-enabled: true
default-team-radar-enabled: true
radar-update-interval-ticks: 20
radar-max-distance: 1000.0
public-radar-enabled: false
```

## Project layout

```text
src/main/java/dev/allyradar/
├── AllyRadar.java                 # Paper lifecycle and cache ownership
├── command/                       # /allyradar, /team, /radar
├── model/                         # Immutable domain values
├── radar/                         # Privacy policy and compatibility provider
└── storage/                       # SQLite schema, migration, and repository
src/test/                          # Policy and persistence tests
docs/                              # User, architecture, testing, and release guides
.github/                           # CI, releases, Dependabot, issues, and PR setup
```

## Development

```bash
./gradlew clean test build
```

The build uses Paper as `compileOnly` and embeds SQLite JDBC into the plugin JAR. Before changing radar, membership, or location behavior, read [`AGENTS.md`](AGENTS.md), [`SECURITY.md`](SECURITY.md), and the applicable reusable skill under [`.opencode/skills`](.opencode/skills).

Contributors should read [`CONTRIBUTING.md`](CONTRIBUTING.md). The repository maintainer requires each changed file to be committed in its own separate Git commit.

## Verification status

- **Implemented:** team lifecycle, invitations, SQLite persistence, privacy policy, action-bar radar, documentation, and GitHub automation.
- **Compiled:** Paper API `26.3.build.238-stable` with Java 25.
- **Automated-tested:** visibility policy, parsing, persistence, restart recovery, invitations, and default preferences.
- **Live integration-tested:** not yet verified in this development environment.
- **Native locator-bar privacy-tested:** not implemented; limitation documented.

## Roadmap and recommended next features

The prioritized roadmap is in [`docs/roadmap.md`](docs/roadmap.md). The most valuable next additions are:

1. An isolated Paper integration test harness covering plugin load and real commands.
2. A GUI for team and radar status, while retaining command parity.
3. Explicit invitation listing and cancellation commands.
4. A verified packet-based locator-bar adapter, only if stable per-recipient filtering becomes supportable.
5. Optional compass tracking for one selected teammate.
6. More migration tests and a documented database backup/restore procedure.

## License

AllyRadar is released under the [MIT License](LICENSE).
