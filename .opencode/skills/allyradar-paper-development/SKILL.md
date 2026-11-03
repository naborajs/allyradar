# AllyRadar Paper development skill

## Purpose
Implement safe Paper lifecycle, command, event, scheduler, and Adventure UI behavior.

## Use when
Changing JavaPlugin startup, Bukkit events, commands, scheduling, or player output.

## Rules

- Target the pinned Paper API and Java 25.
- Access Bukkit state on the server thread.
- Cancel tasks and close the database on disable.
- Keep command errors clear and never include exact unauthorized coordinates.
- Use `plugin.yml` metadata and test that the packaged JAR contains it.

## Verification

Compile, run unit tests, inspect `plugin.yml` inside the JAR, and perform an isolated server smoke test before claiming loaded compatibility.
