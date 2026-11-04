# AllyRadar Complete Setup & Operational Guide

This document is the definitive, step-by-step setup and operational manual for server administrators and players running **AllyRadar** on Paper 26.3 (Java 25+), including hybrid servers with GeyserMC / Floodgate for Bedrock players.

---

## 1. Understanding the Displays: Native Locator Bar vs. HUD

In Minecraft 1.21.6+ (Paper 26.3), Mojang introduced the **Player Locator Bar** on the Experience (XP) Bar:
* **The Dots on the XP Bar**: Waypoint markers showing player headings and elevations.
  * In unmodded vanilla Minecraft, `/gamerule locator_bar true` broadcasts **every online player's dot globally to all players**.
  * **AllyRadar fixes this**: When paired with ProtocolLib, AllyRadar intercepts waypoint packets and ensures **only authorized teammates appear on the XP bar**. Solo players and players on other teams are hidden.
* **The Action Bar / BossBar HUD**: AllyRadar's rich compass HUD showing exact teammate names, compass bearings (N, NE, E, SE, S, SW, W, NW), and rounded distance in blocks.

---

## 2. Server Installation (Quick-Start)

### Requirements:
* **Server**: Paper 26.3 (`paper-api:26.3.build.238-stable` or newer).
* **Java**: Java 25+.
* **Optional but Recommended**: **ProtocolLib 5.4.0+** (enables native XP-bar locator dots filtering).

### Step-by-Step Installation:
1. **Copy JAR**: Place `AllyRadar-1.0.0.jar` into your server's `plugins/` folder.
2. **(Optional for XP Bar Dots)**: Place `ProtocolLib.jar` into your `plugins/` folder.
3. **Start Server**: Run the server to generate `plugins/AllyRadar/config.yml` and `allyradar.db`.
4. **Choose Your Locator Bar Setup**:
   * **Mode 1 (XP Bar Dots + HUD with ProtocolLib)**:
     If ProtocolLib is installed, AllyRadar automatically registers its `TRACKED_WAYPOINT` packet filter. Teammates will see each other's dots on the XP bar, while enemy and solo players are completely blocked. No additional commands required!
   * **Mode 2 (Action Bar / BossBar HUD Only, Without ProtocolLib)**:
     If you do not use ProtocolLib, prevent vanilla Minecraft from leaking players globally on the XP bar by disabling the vanilla gamerule:
     ```text
     /gamerule locator_bar false
     ```
     AllyRadar will use its built-in Action Bar or BossBar HUD to display teammates with 100% privacy.

---

## 3. Configuration (`plugins/AllyRadar/config.yml`)

```yaml
# Database file (stores persistent teams and player privacy settings)
database-file: allyradar.db

# Maximum players allowed per team
max-team-size: 10

# Hours before an unaccepted team invitation expires
invitation-expiration-hours: 48

# Default sharing preference for new players (PRIVATE, TEAM, or PUBLIC)
# Note: A player with TEAM visibility who has no team evaluates to PRIVATE automatically.
default-location-visibility: TEAM

# Default receiving state for new players
default-radar-enabled: true
default-team-radar-enabled: true

# How often the radar refreshes (20 ticks = 1 second)
radar-update-interval-ticks: 20

# Maximum tracking radius in blocks
radar-max-distance: 1000.0

# Radar HUD display mode:
# ACTION_BAR - Direction and distance in player's action bar
# BOSS_BAR   - Direction and distance in a subtle boss bar at the top of the screen
# BOTH       - Render simultaneously in both
# DISABLED   - Disable HUD text, rely only on XP bar dots
radar-display-mode: ACTION_BAR

# Native Locator Bar (XP Bar waypoint dots) filtering
locator-bar-filter-enabled: true

# Allow public radar tracking (enabled by default so solo players who run /ally public can see each other)
public-radar-enabled: true
```

---

## 4. Player Commands & How to Play

All commands can be executed easily using the main `/ally` command.

### A. Visibility Controls (Who can see you?):
* `/ally private` (or `/ally hide`) — **Hide your location completely** from EVERYONE on the server. You are 100% invisible on HUD radar and XP bar dots.
* `/ally team` — Share your location **ONLY with your teammates**. If you have no team (solo), your location remains hidden.
* `/ally public` — Share your location with **EVERYONE on the server**. Any nearby player with radar turned on can see your direction and distance.

### B. Radar Screen & Display Controls (Can you see others?):
* `/ally on` — Turn **ON** radar screen & share location with teammates.
* `/ally off` — Turn **OFF** radar screen & hide your location completely (`PRIVATE`).
* `/ally hud [on|off]` — **Toggle or turn off the text at the bottom of the screen** (Action Bar HUD). If you prefer only using the XP bar dots and find the text intrusive, run `/ally hud off`!
* `/ally radius <meters>` — **Customize your radar detection distance** (e.g. `/ally radius 50` to only see players within 50 blocks). Run `/ally radius reset` to return to server maximum.

### C. Checking Your Status:
* `/ally status` — View a clear, friendly summary: your current team, who can see your location, radar master state, text HUD state, and detection radius.
* `/ally who` — Lists all players currently visible to you and their distance in blocks (filtered by your detection radius).

### D. Team Management:
* `/ally create <team_name>` — Create a new team with you as the owner.
* `/ally invite <player>` — Invite a player to join your team. The invited player will receive an immediate notification in chat.
* `/ally accept` (or `/ally join`) — Accept a pending team invitation with zero arguments.
* `/ally deny` — Decline a pending team invitation.
* `/ally leave` — Leave your team. If you are the only member, this automatically deletes the team cleanly.
* `/ally list` — View all teammates in your team.
* `/ally info` — View team details, member count, and team radar state.
* `/ally kick <player>` — Remove a member from your team (owner only).
* `/ally transfer <player>` — Transfer team ownership to another member (owner only).
* `/ally disband` (or `/ally delete`) — Disband the team completely (owner only).

---

## 5. GeyserMC & Floodgate (Bedrock Touch & Controller Support)

AllyRadar is 100% optimized for Bedrock players on mobile touchscreens and console controllers:
* **Short Shorthand `/a`**: Bedrock players can use `/a` instead of typing `/ally`:
  * `/a on` — Turn on radar
  * `/a off` — Turn off radar
  * `/a hud` — Toggle bottom text on/off
  * `/a radius 50` — Set radar range to 50 meters
  * `/a public` — Share with all (solo)
  * `/a private` — Hide completely
  * `/a accept` — Accept team invite
  * `/a leave` — Leave team
  * `/a status` — View status
  * `/a bedrock` — View Bedrock guide
* Bedrock usernames starting with `.` (e.g. `.Aloneboy18k`) are fully supported for invitations and team commands.
* Wiping cleared radar action bars uses whitespace packets so Bedrock clients immediately clear stale HUD text.

---

## 6. Pre-Release Acceptance Checklist

Before launching on a public production server:
1. Verify build passes all automated tests: `./gradlew clean test build`.
2. Confirm JAR SHA-256 matches expected build artifact.
3. Test two accounts:
   - Account A creates team, invites Account B.
   - Account B types `/ally accept` and confirms both are visible to each other.
   - Account B types `/ally private` and confirms Account A immediately loses tracking (both on HUD and XP bar dots).
   - Account B leaves team (`/ally leave`) and confirms neither player sees the other.
4. Verify non-teammates on another team (or solo players) never appear on each other's screens.
