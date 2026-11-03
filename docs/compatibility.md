# Compatibility matrix

| AllyRadar | Paper API | Java | Build | Automated tests | Live integration | Radar |
|---|---|---:|---|---|---|---|
| 1.0.0 | 26.3.build.238-stable | 25+ | Verified | Pass | Not run | Action-bar provider compiled; privacy policy tested |
| Future | Verify first | Verify first | Pending | Pending | Pending | Pending |

Paper's version service reports Minecraft 26.3 as supported, with Java 25 minimum and build 238 available. The build pins `paper-api:26.3.build.238-stable`. This repository does not claim support for older Minecraft releases.

## Upgrade procedure

1. Load the compatibility upgrade skill.
2. Verify the new Paper API, server build, Java requirement, and mapping model from Paper sources.
3. Compile against the exact verified dependency.
4. Keep team, SQLite, command, and privacy policy code unchanged where possible.
5. Update only compatibility adapters and the matrix.
6. Run tests, build, inspect the JAR, and perform an isolated server smoke test.
7. Record compiled, loaded, automated-tested, radar-tested, and privacy-tested status separately.
