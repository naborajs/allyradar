# Release procedure

1. Run `./gradlew clean test build`.
2. Inspect `build/libs/AllyRadar-<version>.jar` and verify `plugin.yml` metadata.
3. Update `CHANGELOG.md`, README, and compatibility status.
4. Review `SECURITY.md` and perform an isolated server smoke test.
5. Commit each changed file separately, as required by the project workflow.
6. Obtain explicit authorization before tagging, pushing, or publishing a release.

The GitHub Actions workflow builds and uploads the JAR on CI. It does not publish a public release automatically.
