# Security and privacy

## Guarantees implemented

- Player identity is stored by UUID, not by mutable username.
- SQL values use prepared statements and SQLite foreign keys.
- Location output is authorized server-side before rendering.
- PRIVATE always denies location sharing.
- Team owners cannot override an individual visibility preference.
- Exact coordinates are not included in command or radar output.
- Team membership is unique per player.
- Invitations expire and are accepted only for the invited UUID.

## Offline-mode note

Offline-mode UUIDs are derived from usernames by the server and are not equivalent to authenticated online-mode identity. Servers using offline mode should document that a username change can result in a different effective UUID. AllyRadar does not provide authentication.

## Review requirements

Changes involving location, packets, commands, membership, or persistence must check every disclosure path: action bar, commands, GUI, logs, plugin messages, and APIs. Never add exact coordinates to debug logs. Do not use a UI-only hide as a privacy boundary.
