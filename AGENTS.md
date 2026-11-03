# AllyRadar development instructions

## Scope

AllyRadar is a Java 25 Paper plugin targeting Paper 26.3. Development stays in this repository and never touches a live Minecraft server.

For installation, server operation, AI-assisted control, acceptance testing, release gates, and the standard agent report, read `AGENT_GUIDE.md`.

## Architecture

- `dev.allyradar.model` contains immutable, platform-independent values.
- `dev.allyradar.storage` owns SQLite schema, migrations, prepared statements, and UUID identity.
- `dev.allyradar.radar` contains authorization policy and the compatibility provider interface. The current provider renders a filtered action bar.
- `dev.allyradar.command` contains `/allyradar`, `/team`, and `/radar` command handling.
- `dev.allyradar.AllyRadar` is the Paper lifecycle/bootstrap class.

Keep team, preference, and privacy rules independent from version-sensitive server internals. A future locator-bar packet adapter must implement a compatibility interface and must pass privacy review before activation.

## Coding rules

- Use Java 25, records for immutable data, UUIDs for player identity, and prepared SQL statements.
- Keep Bukkit/Paper calls on the server thread. Do not log exact coordinates.
- PRIVATE visibility always wins. Team owners cannot override a player's sharing preference.
- Never use `hidePlayer` as a privacy control; it changes gameplay visibility rather than filtering location data.
- Validate command input and centralize authorization in repository/policy methods.
- Add a focused automated test for new policy, persistence, or compatibility behavior.

## Build and test

```bash
./gradlew clean test build
```

The build produces `build/libs/AllyRadar-1.0.0.jar`. The JAR embeds SQLite JDBC and depends on the Paper API supplied by the server.

## Compatibility procedure

Read `.opencode/skills/allyradar-minecraft-compatibility-upgrade/SKILL.md`, verify the Paper release and Java requirement from authoritative sources, update only compatibility code, then run tests and build. Update `docs/compatibility.md` and `CHANGELOG.md` with verified versus unverified results.

## Git workflow

Use conventional commit subjects. This project follows the maintainer's rule: **each file is committed in its own separate Git commit**. Never commit credentials, server data, worlds, tokens, or local database files.

## Security and production safety

Review `SECURITY.md` before changing location or membership behavior. Do not install, deploy, restart, or configure AllyRadar on a production server without explicit approval.

## Open-source repository

The public-repository standards are in `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, and `LICENSE`. GitHub automation lives under `.github/`: CI builds and tests, release automation is tag-driven, dependency review checks pull requests, and Dependabot updates Gradle and action dependencies. Do not push a release tag without maintainer approval.

## Skills

Load the relevant skill under `.opencode/skills/` before making architecture, Paper compatibility, privacy, security, testing, or release changes.
