# AllyRadar security skill

## Purpose
Review identity, authorization, persistence, command input, and privacy boundaries.

## Use when
Changing team ownership, invitations, UUID handling, database code, permissions, or output.

## Rules

- Use UUIDs for identity and derive the actor from the online player.
- Use prepared statements and transactions for multi-step membership changes.
- Validate names, enums, team size, and invitation expiry.
- Enforce deny-by-default authorization in repository/policy methods.
- Never commit credentials, databases, worlds, or server configuration.

## Verification

Test cross-team access, duplicate membership, expired invitations, ownership transfer, malformed input, SQL metacharacters, and privacy revocation.
