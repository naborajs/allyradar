# Security and privacy

For a suspected vulnerability, do not open a public issue. Use a private GitHub Security Advisory at [github.com/naborajs/allyradar/security/advisories/new](https://github.com/naborajs/allyradar/security/advisories/new) when available. Include the affected version, safe reproduction steps, and privacy impact without attaching credentials, worlds, databases, or exact player coordinates.

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

Supported security baseline: the current `1.0.x` release line on the verified Paper/Java combination in `docs/compatibility.md`.
