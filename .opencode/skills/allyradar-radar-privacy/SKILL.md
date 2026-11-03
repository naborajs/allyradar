# AllyRadar radar privacy skill

## Purpose
Prevent unauthorized location disclosure in every radar output path.

## Use when
Changing visibility, team radar, action-bar output, locator-bar support, compass tracking, or GUI behavior.

## Rules

- Evaluate the tracked player's preference before creating output.
- PRIVATE always denies; TEAM requires same team and team radar enabled; PUBLIC requires server opt-in.
- Respect the receiver's radar preference and world boundaries.
- Never use client/UI hiding as the privacy boundary.
- Do not display or log exact coordinates.

## Verification

Test private, team, public, cross-team, receiver-off, team-off, cross-world, and revoked-membership cases. Review all commands, logs, packets, and APIs for coordinate leakage.
