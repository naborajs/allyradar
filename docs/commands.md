# Command reference

All commands require `allyradar.use`, which defaults to true. Player commands must be run in-game.

## `/team`

| Command | Permission | Behavior |
|---|---|---|
| `/team create <name>` | `allyradar.use` | Creates a team and makes the player owner. Names are 2–24 characters using letters, numbers, spaces, `_`, or `-`. |
| `/team invite <player>` | `allyradar.use` | Owner-only invitation by UUID resolved from the player name. Invitations expire according to configuration. |
| `/team accept <team>` | `allyradar.use` | Accepts an active invitation for the online player. |
| `/team deny <team>` | `allyradar.use` | Removes the player's invitation. |
| `/team leave` | `allyradar.use` | Leaves as a member. The owner must transfer ownership or disband first. |
| `/team kick <player>` | `allyradar.use` | Owner-only removal of a member. |
| `/team transfer <player>` | `allyradar.use` | Owner-only transfer to an existing member. |
| `/team info` | `allyradar.use` | Shows team name, size, and team radar state. |
| `/team list` | `allyradar.use` | Lists current members and online status. |
| `/team settings radar <on\|off>` | `allyradar.use` | Owner-only team radar switch. It never overrides an individual `PRIVATE` setting. |
| `/team disband` | `allyradar.use` | Owner-only permanent team removal. |

## `/radar`

| Command | Behavior |
|---|---|
| `/radar on` | Enables radar receiving for the player. |
| `/radar off` | Disables all radar output for the player. |
| `/radar visibility private` | Shares no location through AllyRadar. |
| `/radar visibility team` | Shares only with authorized same-team receivers. |
| `/radar visibility public` | Shares when the server's public-radar opt-in is enabled. |
| `/radar status` | Shows sharing and receiving preferences. |
| `/radar who` | Lists currently authorized same-dimension online targets. |

## `/allyradar`

`/allyradar` displays help. `/allyradar reload` requires `allyradar.admin` and reloads non-startup configuration values. The database connection and radar scheduler are initialized at startup.

## Privacy behavior

Commands never provide exact coordinates. A target is omitted before output when the target is `PRIVATE`, the receiver has radar disabled, team radar is off, public sharing is not enabled, the players are in different worlds, or the target is outside the configured range.
