# Configuration

```yaml
database-file: allyradar.db
max-team-size: 10
invitation-expiration-hours: 48
default-location-visibility: TEAM
default-radar-enabled: true
default-team-radar-enabled: true
radar-update-interval-ticks: 20
radar-max-distance: 1000.0
public-radar-enabled: false
debug: false
```

- `database-file`: file below the plugin data folder.
- `max-team-size`: minimum effective value is 2.
- `invitation-expiration-hours`: lifetime of new invitations.
- `default-location-visibility`: `PRIVATE`, `TEAM`, or `PUBLIC` for new players.
- `default-radar-enabled`: whether new players receive radar output.
- `default-team-radar-enabled`: reserved default for future team creation settings.
- `radar-update-interval-ticks`: action-bar refresh interval.
- `radar-max-distance`: maximum same-dimension tracking distance.
- `public-radar-enabled`: explicit server opt-in for PUBLIC sharing.
- `debug`: reserved for future non-sensitive diagnostics.

Unsafe low values are clamped at startup: team size and invitation lifetime cannot be below their safe minimums, and radar interval and range cannot be zero or negative. Startup-only values are not changed by `/allyradar reload`.
