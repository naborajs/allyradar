# AllyRadar Minecraft compatibility upgrade

## Purpose
Upgrade Paper support without rewriting stable team and privacy behavior.

## Use when
Paper or Minecraft releases a new supported version or changes locator-bar APIs.

## Procedure

1. Read `AGENTS.md` and `docs/compatibility.md`.
2. Verify the Paper API coordinate, build, Java requirement, and mapping model from Paper sources.
3. Research stable APIs and packet changes; do not invent coordinates or methods.
4. Change only compatibility providers and build metadata where possible.
5. Run tests and `./gradlew clean test build`.
6. Perform isolated server loading and privacy tests.
7. Record compiled, loaded, automated-tested, radar-tested, and privacy-tested status separately.
8. Update `CHANGELOG.md` and the compatibility matrix.

## Safety

If per-recipient native locator-bar filtering cannot be verified, retain the filtered action-bar fallback and document the limitation.
