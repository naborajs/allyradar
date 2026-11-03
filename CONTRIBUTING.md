# Contributing to AllyRadar

Thank you for helping improve AllyRadar. Contributions are welcome for the Java code, tests, documentation, compatibility research, and isolated Paper testing.

## Before you start

1. Read `AGENTS.md`, `SECURITY.md`, and the relevant skill under `.opencode/skills/`.
2. Check existing issues and pull requests before opening new work.
3. Keep development isolated from live Minecraft servers and worlds.
4. Never include tokens, credentials, server databases, worlds, or private configuration.

## Development setup

Requirements:

- Java 25+
- Gradle 9.8+ or the included Gradle wrapper
- Network access to Maven Central and the Paper Maven repository

Run the complete local check:

```bash
./gradlew clean test build
```

The JAR is written to `build/libs/AllyRadar-1.0.0.jar`.

## Code expectations

- Keep stable team, SQLite, and privacy logic independent from version-specific Paper internals.
- Use UUIDs and prepared SQL statements.
- Treat `PRIVATE` as an unconditional deny for location disclosure.
- Do not log exact coordinates or use UI hiding as a privacy boundary.
- Add or update focused tests for behavior changes.
- Document compatibility changes in `docs/compatibility.md` and `CHANGELOG.md`.

## Pull requests

Use a clear title and explain the user-visible behavior, security/privacy impact, tests run, and any manual Paper testing. Keep pull requests focused. Do not claim a Minecraft version is supported without compiling and testing it in an isolated server.

The repository maintainer currently requires each changed file to be committed in its own separate Git commit.

## Reporting security issues

Do not disclose security or location-privacy issues in a public issue. Follow `SECURITY.md` and use a private GitHub Security Advisory when available.
