# Development

## Requirements

- Java 25+
- Gradle 9.8+
- Network access to Maven Central and the Paper Maven repository

## Checks

```bash
./gradlew clean test build
```

The build uses the pinned Paper API and embeds SQLite JDBC in the plugin JAR. Test classes cover visibility authorization, parsing, team persistence, invitations, and restart recovery.

## Manual smoke test

Use an isolated Paper 26.3 server with two test accounts:

1. Create a team and invite the second account.
2. Accept the invitation and confirm `/team list`.
3. Test `TEAM`, `PRIVATE`, and `PUBLIC` with the server public opt-in on and off.
4. Disable receiver radar and confirm no action-bar output.
5. Disable team radar and confirm team targets disappear.
6. Change worlds and confirm cross-dimension targets are omitted.

Do not run this against a live server.
