# Release procedure

1. Run `./gradlew clean test build`.
2. Inspect `build/libs/AllyRadar-<version>.jar` and verify `plugin.yml` metadata.
3. Update `CHANGELOG.md`, README, and compatibility status.
4. Review `SECURITY.md` and perform an isolated server smoke test.
5. Commit each changed file separately, as required by the project workflow.
6. Obtain explicit authorization before tagging, pushing, or publishing a release.
7. Create a matching tag such as `v1.0.0`; `.github/workflows/release.yml` verifies the tag, rebuilds, generates `SHA256SUMS`, and creates the GitHub release.

The release workflow is ready but has not been triggered from this workspace. A maintainer must review the generated files and authorize the tag push.

The GitHub Actions workflow builds and uploads the JAR on CI. It does not publish a public release automatically.
