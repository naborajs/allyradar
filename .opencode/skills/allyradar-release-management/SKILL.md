# AllyRadar release management skill

## Purpose
Prepare a reproducible, documented Paper plugin artifact without unauthorized publication.

## Use when
Changing the version, preparing a tag, updating CI, or creating a release candidate.

## Rules

- Keep semantic version in Gradle and expand it into `plugin.yml`.
- Run the full test/build check and inspect the JAR contents.
- Update README, CHANGELOG, compatibility, and known limitations.
- Commit every file separately according to `AGENTS.md`.
- Ask before pushing tags or publishing releases.

## Verification checklist

Confirm artifact path, plugin metadata, embedded SQLite driver, test results, compatibility status, clean Git state, and absence of secrets. CI may build and upload artifacts but must not publish automatically.
