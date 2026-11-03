# Roadmap and feature recommendations

This list separates high-value improvements from features that require risky version-specific work.

## Next priority: integration confidence

1. Run a real Paper 26.3 server smoke test with two accounts.
2. Add an automated plugin-load/integration harness.
3. Test join/quit, restart recovery, invitation expiry, ownership transfer, and cross-world privacy.
4. Add a supported-version test matrix when another Paper target is intentionally supported.

## High-value product improvements

- `/team invitations` and `/team cancel-invite <player>` for invitation visibility and cleanup.
- Confirmation prompts for `/team disband` and ownership transfer.
- A compact inventory GUI for team members, radar status, and visibility selection.
- Optional per-player radar range and display mode.
- A one-target compass mode with explicit consent and clear status feedback.
- Configurable messages and MiniMessage formatting.
- Database backup/restore commands for administrators, with no coordinate logging.

## Compatibility work

- Research a packet adapter only after confirming a stable library supports the exact Paper/client version.
- Keep the action-bar fallback as the safe default.
- Add a resolver that disables unsupported native radar behavior rather than silently weakening privacy.

## Operational improvements

- Add automated artifact signing and published checksums after a release key process is chosen.
- Add a staging Paper server workflow for maintainers.
- Add performance measurements for larger online-player counts.
- Add migration fixtures for every future schema version.

## Features intentionally deferred

Historical location tracks, offline location queues, cross-server synchronization, and forced owner tracking are intentionally deferred because they increase privacy, retention, and authorization risk.
