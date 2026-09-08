# Parcade Dragon Escape checkpoint

This `Parcade-DE` branch preserves the legacy Minecraft Java 1.8.8 Parcade experience while it is reworked. It does not replace `main` or merge other DE versions.

## Included

- `decompiled_output/`: existing decompiled DE source, with the maintained performance fixes applied.
- `DragonEscape.jar`: same packaged build as `server/plugins/Dragon Escape.jar`.
- `server/`: Spigot, the active plugin JARs, sanitized configuration templates. Parcade, Link, Cubics, PermissionsEx and DragonEscape remain enabled.
- `maps/DE/` and `maps/dragonescape/`: world templates, with player files and scoreboard records removed. `server/plugins/DragonEscape/schematics/` contains the Dragon Escape schematic assets for instantiated arenas.
- `patches/parcade/`: minimal XP leaderboard patch (configured database, small/empty pages).
- `tools/`: local SQL bootstrap source, build/launch helpers and real-server integration checks.

Runtime logs, databases, player files, operator/ban lists, backups, crash dumps, generated game arenas, telemetry IDs and local credentials are excluded. Region files retain map entities, blocks and tile data; this is a map snapshot, not a forensic anonymization of player-authored signs/books. Existing third-party binaries are preserved as dependencies, not relicensed by this repository; redistribution/use remains subject to their original terms.

## Run locally

Requirements: Python 3, Java **8**, Docker with support for Linux containers. No Python packages are required by the launcher.

Windows example:

```bat
set "JAVA8=C:\Program Files\Java\jre-1.8\bin\java.exe"
python tools\run-local.py
```

The first run creates an ignored `runtime/` and random credentials in ignored `.local-db.json`. Read the Minecraft EULA and set `eula=true` in `runtime/eula.txt` **only if you agree**, then rerun. Connect a signed-in Minecraft Java 1.8.8/1.8.9 client to `127.0.0.1:25565`. Do not run alongside another server on that port.

MariaDB uses container/volume `parcade-de-checkpoint-db` on **127.0.0.1:3308**, separate from the development server's database. Keep `.local-db.json` with that volume; deleting only one will break authentication. Type `stop` in the console to save/shut down. Stop SQL afterward with `docker stop parcade-de-checkpoint-db`. Do not use `/reload` or PlugMan to reload these legacy world plugins.

Templates have `CHANGE_ME` passwords; never run directly inside `server/`. The launcher initializes runtime configs, but does not overwrite an existing runtime or its worlds. To deploy rebuilt JARs, stop that runtime, copy the rebuilt JARs from `server/plugins/`, then restart. Operator/rank records are deliberately not shipped; use console `op <name>` and local PermissionsEx configuration as appropriate. The inherited `op-permission-level=0` is retained; change it deliberately if vanilla administrative command levels are desired.

## Build maintained changes

Set `JAVA_HOME` to JDK 9+ (tested with JDK 23) or put `javac` on PATH:

```bat
python tools\build.py
```

Compiles for Java 8 and replaces only the ten maintained DE source classes/their inner classes, the Parcade leaderboard classes, and LocalPlayers. Remaining classes/resources come from the supplied JARs. This is a reproducible **patch build**, not a claim that the entire decompiled codebase builds cleanly. Binary default database configs are sanitized too.

## What changed

- Ported the original Mineplex Dragon Escape leap vector into the separate **Mineplex Leap** kit for solo A/B testing. It uses four charges, an eight-second cooldown, Mineplex's bounding-box grounded boost, and fixed reference physics; the existing Parcade Leap remains unchanged.
- Removed 50 untracked startup schematic pastes; games allocate/reuse arenas on demand.
- Serialized off-thread schematic loading, closed resources, and replaced fixed readiness timers with paste completion/failure states.
- Budgeted idle/public arena restoration (shared 256-block / approximately 2-ms scheduling allowance) and preserved tile-entity data.
- Fixed active-game removal, duplicate end cleanup, retained player/inventory references and blanket chunk-unload cancellation.
- Reduced public snapshot allocation and scoreboard writes; stopped late joins/damage after round end.
- Reserved compatible restoring arenas exclusively instead of pasting replacements unnecessarily; failed restores are quarantined.
- Reduced FAWE `extra-time-ms` from 0 to -35, trading longer preparation for less contention. This is a soft allowance, not a hard tick-time limit.
- LocalPlayers initializes missing settings/stats/player/currency schema without replacing Parcade NPC/menu/sidebar behavior.

## Verification and remaining limitations

Run the deterministic Mineplex leap-vector checks with:

```bat
python tools\test-leap.py
```

The vector, height cap, grounded boost, and four-charge constants match Mineplex's `PerkLeap("Leap", 1, 1, 8000, 4)`/`UtilAction.velocity` implementation. Parcade's server does not contain Mineplex's custom velocity packet reapplication event, so live side-by-side testing remains the final check for any network-level movement difference.

20 isolated real-server checks passed before packaging: 1,000 schematic block/data comparisons, chest-content preservation, 600-block restoration, failed-paste handling, exclusive restoring-arena reservations, reuse, and idempotent public cleanup. `tools/PerformanceChecks.java` is the test plugin source; run the isolated test with `python tools/test.py` after configuring JAVA8/JAVA_HOME and initializing local SQL via the launcher. It deliberately tests a corrupt schematic and should log that expected error.

The live build was cleanly restarted and answered Minecraft status queries. This is not a controlled TPS benchmark: first-use/simultaneously occupied private arenas still require pastes, active solo resets remain synchronous, and occasional long ticks remain (about 167 ms in the last isolated run). Full two-player public plus Digger gameplay has not been automated. Hub/Parkour transfers and Discord linking need their separate old-network services. Legacy update/Ebean/geolocation warnings may remain. No historical network ranks, player balances, or scores were imported.

### Binary audit

The supplied FAWE 2019 JAR contains an unreadable `linux/aarch64/libzstd.so` ZIP entry. It is preserved byte-for-byte from the working Windows installation; do not assume Linux ARM64 support. All other active plugin JAR entries passed ZIP integrity checks. The packaged build and all 20 integration checks were rerun successfully on Windows/Java 8 before this checkpoint.

## Additional preserved server assets

WorldEdit schematics, PlaceholderAPI expansions, fonts, cosmetic songs, Parkour map definitions, WorldGuard region/configuration files, and other plugin configuration assets are included. Historical Parkour leaderboards, personal bests, player records, WorldGuard region memberships, editor sessions/caches, and database contents are excluded. Configurations for plugins not currently installed are preserved but do not enable those plugins. Disabled/backup JARs and the duplicate world archive are not installed. World data retains authored blocks, entities, signs and books; it is not a forensic anonymization.

The launcher defaults to loopback-only access. For remote self-hosting, configure `runtime/server.properties` deliberately and retain online authentication; external Hub/Parkour/Discord services are not supplied.
