# AllyRadar agent and server-operator guide

This is the operational source of truth for AI assistants, automation agents, and server operators working with AllyRadar. Read it before installing, changing, upgrading, or troubleshooting the plugin.

## What AllyRadar does

AllyRadar provides persistent UUID-based teams, expiring invitations, ownership controls, consent-based location sharing, and a filtered action-bar radar. The sharing modes are:

- `PRIVATE`: nobody can receive the player's AllyRadar location.
- `TEAM`: only authorized same-team receivers can receive it while team radar is enabled.
- `PUBLIC`: sharing is allowed only when the server enables public radar.

The receiver can disable their own radar. The current radar is a custom filtered action bar showing authorized names, relative direction, and rounded distance. It is **not** private vanilla locator-bar support; stable Paper APIs do not provide the required per-recipient filtering.

## Source-of-truth map

Read these files in order:

1. `AGENTS.md` — development rules, architecture, security, and Git workflow.
2. `AGENT_GUIDE.md` — this operational guide.
3. `README.md` — public overview and quick start.
4. `docs/installation.md` — installation and first-run test.
5. `docs/commands.md` — commands and permissions.
6. `docs/configuration.md` — configuration and reload behavior.
7. `docs/compatibility.md` — verified Paper and Java support.
8. `SECURITY.md` — privacy boundaries and vulnerability reporting.
9. `docs/release.md` — release and artifact procedure.
10. `.opencode/skills/` — focused development and review instructions.

For code changes, then inspect `src/main/java`, `src/test/java`, `build.gradle.kts`, and `src/main/resources`.

## Verified target

| Item | Current value |
|---|---|
| Plugin | AllyRadar `1.0.0` |
| Paper API | `26.3.build.238-stable` |
| Java | 25+ |
| Artifact | `build/libs/AllyRadar-1.0.0.jar` |
| Radar | Filtered action bar |
| Native locator-bar privacy | Not implemented |

Report these states separately: `compiled`, `automated-tested`, `loaded-tested`, `gameplay-tested`, and `privacy-tested`. Never claim a server test that was not run.

## Safety contract

First identify the environment as `development`, `staging`, or `production`.

### Development

Agents may read and edit repository files, run Gradle checks, inspect the JAR, and create separate Git commits per changed file.

### Staging or isolated server

After confirming the server path and backup plan, agents may copy the JAR, start or restart the isolated server, run the two-player test, and inspect redacted logs.

### Production

Ask for explicit approval before copying the JAR, stopping or restarting the server, editing production configuration, changing or deleting the database, running destructive team commands, pushing a release tag, or publishing a release.

Never request, print, commit, or store credentials, RCON passwords, worlds, private server configuration, player databases, or exact coordinates.

## Build and inspect locally

From the repository root:

```bash
./gradlew clean test build
```

Expected result: `BUILD SUCCESSFUL`.

Inspect the artifact:

```bash
unzip -p build/libs/AllyRadar-1.0.0.jar plugin.yml
unzip -p build/libs/AllyRadar-1.0.0.jar META-INF/MANIFEST.MF
sha256sum build/libs/AllyRadar-1.0.0.jar
```

The metadata must identify `AllyRadar`, version `1.0.0`, main class `dev.allyradar.AllyRadar`, and API version `26.3`.

## Install on an isolated Paper server

Do not guess the server path. Set it explicitly:

```bash
SERVER_DIR=/absolute/path/to/test-server
PLUGIN_DIR="$SERVER_DIR/plugins"
ARTIFACT="$PWD/build/libs/AllyRadar-1.0.0.jar"
test -f "$ARTIFACT"
test -d "$PLUGIN_DIR"
cp "$ARTIFACT" "$PLUGIN_DIR/AllyRadar-1.0.0.jar"
```

Procedure:

1. Confirm Java 25+ with `java -version`.
2. Confirm the Paper version from the server log or `version` command.
3. Stop the isolated server before copying the JAR.
4. Back up `plugins/AllyRadar/` when upgrading.
5. Copy the JAR and start the server.
6. Confirm the log contains `AllyRadar enabled`.
7. Confirm `plugins/AllyRadar/config.yml` and `allyradar.db` exist.
8. Run the acceptance test below.

Never copy `build/`, a development database, or a production database into source control.

## Control commands

### Server console

The console can run:

```text
allyradar
allyradar reload
```

`allyradar reload` requires `allyradar.admin`. The database path and radar scheduler interval are startup-only. The console cannot run `/team` or `/radar` because those commands require a player identity.

### Player commands

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

/radar on
/radar off
/radar visibility <private|team|public>
/radar status
/radar who
```

Permissions:

| Permission | Default | Purpose |
|---|---:|---|
| `allyradar.use` | true | Use `/team` and `/radar`. |
| `allyradar.admin` | op | Use `/allyradar reload`. |

No permission overrides `PRIVATE`. Team ownership is not a privacy override. See `docs/commands.md` for exact behavior.

## First-run acceptance test

Use two disposable players, Alice and Bob, on the isolated server:

1. Alice: `/team create Explorers`.
2. Alice: `/team invite Bob`.
3. Bob: `/team accept Explorers`.
4. Both: `/team info` and `/team list`.
5. Bob: `/radar status`; confirm `TEAM` sharing.
6. Alice: `/radar who`; confirm Bob is visible in the same world.
7. Bob: `/radar visibility private`; Alice must no longer see Bob in `/radar who` or the action bar.
8. Bob: `/radar visibility team`; restore sharing.
9. Alice: `/team settings radar off`; team output must stop.
10. Alice: `/team settings radar on`; authorized output may return.
11. Alice: `/radar off`; Alice must receive no radar output.
12. Move one player to another world; they must disappear from radar.
13. Restart the isolated server and confirm membership persists.
14. Test an expired invitation and confirm it cannot be accepted.

Record each result without recording exact coordinates.

## Configuration, backup, and rollback

The generated configuration is `plugins/AllyRadar/config.yml`. Keep `public-radar-enabled: false` unless public sharing is an intentional server policy. Read `docs/configuration.md` before editing. Restart for startup-only values; use `allyradar reload` only for safely reloadable values.

Back up while the server is stopped:

```bash
SERVER_PLUGIN_DIR=/absolute/path/to/server/plugins/AllyRadar
tar -czf "AllyRadar-backup-$(date +%Y%m%d-%H%M%S).tar.gz" "$SERVER_PLUGIN_DIR"
```

For rollback, restore the previous JAR and its matching database backup together. Do not roll a database backward across an untested migration by replacing only the JAR.

## Release gate

Report each item as passed, failed, or not run:

- [ ] `./gradlew clean test build` passed.
- [ ] JAR metadata and checksum inspected.
- [ ] No secrets, worlds, databases, or server files tracked.
- [ ] Paper and Java versions recorded in `docs/compatibility.md`.
- [ ] Isolated server load passed.
- [ ] Two-player privacy acceptance test passed.
- [ ] Upgrade and rollback reviewed.
- [ ] README, changelog, and operator docs are current.
- [ ] Every changed file has its own Git commit.
- [ ] No release tag pushed without approval.

## Standard agent report

```text
Environment: development | staging | production
Paper: <version or not verified>
Java: <version or not verified>
Change: <short summary>
Artifact: <path or not built>
Build: passed | failed | not run
Automated tests: passed | failed | not run
Server load test: passed | failed | not run
Privacy test: passed | failed | not run
Git status: clean | changes present
Push/release: not attempted | completed | blocked
Known limitations: <list>
Next safe action: <one concrete action>
```

## Recommended next features

The best next additions are a real Paper integration harness, `/team invitations` and cancellation, confirmation prompts for destructive actions, a small inventory GUI, optional one-target compass tracking, and documented database backup tooling. Defer historical tracking, offline location queues, forced owner tracking, and cross-server synchronization until their retention and authorization designs are reviewed.
