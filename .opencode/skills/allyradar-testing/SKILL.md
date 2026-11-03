# AllyRadar testing skill

## Purpose
Provide repeatable unit, persistence, build, and isolated Paper verification.

## Use when
Adding behavior, changing schema, upgrading dependencies, or preparing a release.

## Rules

- Run `./gradlew clean test build`.
- Test policy separately from Bukkit integration.
- Test SQLite from a fresh file and after close/reopen.
- Distinguish compiled, automated-tested, server-loaded, radar-tested, and privacy-tested results.
- Never weaken or delete a test to hide a failure.

## Minimum cases

Team creation, invitation, acceptance, expiry, persistence, PRIVATE/TEAM/PUBLIC policy, receiver-off, team-off, cross-team denial, and malformed visibility input.
