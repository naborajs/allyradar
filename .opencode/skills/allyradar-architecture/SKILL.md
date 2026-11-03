# AllyRadar architecture skill

## Purpose
Keep the stable team, storage, policy, and command layers independent from Paper-version code.

## Use when
Adding modules, changing public interfaces, or introducing a compatibility adapter.

## Rules

- Keep UUID identity, SQLite, preferences, and `RadarPolicy` version-independent.
- Put Paper calls in the bootstrap, command, event, or radar provider layer.
- Prefer immutable records and narrow interfaces.
- Do not add NMS or packet dependencies without a compatibility and privacy review.

## Verification

Run `./gradlew clean test build`, inspect the dependency tree, and update `docs/architecture.md` when boundaries change.
