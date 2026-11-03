# AllyRadar AI assistant instructions

Before changing this repository:

1. Read `AGENTS.md` for development rules.
2. Read `AGENT_GUIDE.md` for installation, server control, testing, and reporting.
3. Read `SECURITY.md` before changing team, radar, location, command, packet, or database behavior.
4. Read the relevant skill under `.opencode/skills/`.

## Project facts

- Java 25 Paper plugin targeting Paper `26.3.build.238-stable`.
- Main class: `dev.allyradar.AllyRadar`.
- Artifact: `build/libs/AllyRadar-1.0.0.jar`.
- Current radar: filtered action bar; native per-recipient locator-bar filtering is not implemented.
- Stable identity: player UUIDs.
- Persistence: SQLite with prepared statements and migrations.

## Required behavior

- `PRIVATE` always denies location sharing.
- `TEAM` requires same-team authorization and team radar enabled.
- `PUBLIC` requires the server public-radar opt-in.
- Receiver radar off must suppress all radar output.
- Do not disclose exact coordinates in commands, GUI, logs, plugin messages, or APIs.
- Do not use `hidePlayer` as a privacy boundary.
- Keep Bukkit/Paper access on the server thread.

## Verification

Run:

```bash
./gradlew clean test build
```

Report compiled, automated-tested, server-loaded, gameplay-tested, and privacy-tested status separately. Never claim live server verification if it was not run.

## Operations

For installation, upgrades, backups, player commands, console commands, release checks, and the standard completion report, follow `AGENT_GUIDE.md`. Do not deploy to production, push tags, or publish releases without explicit maintainer approval.
