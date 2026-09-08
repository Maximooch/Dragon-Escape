# Dragon Escape Hybrid Plan

## Goal

Build toward the best practical Dragon Escape implementation by preserving exact Mineplex gameplay mechanics while using Parcade's stronger solo/practice workflow, historical maps, lobby assets, and familiar plugin shell.

The immediate product has two modes:

1. **MPS Dragon Escape** — normal multiplayer games with host/co-host controls.
2. **DE Solo** — static parkour/speedrunning practice with selectable maps and kits, especially Leaper, without public matchmaking or bulk arena provisioning.

## Architectural direction

Use the Parcade Dragon Escape repository as the experimentation shell. Port original Mineplex mechanics into it deliberately without importing Mineplex's production infrastructure.

Use Mineplex-reborn as the behavioral reference for:

- Leap velocity and charge semantics
- Dragon movement and scoring
- Original kits
- Shortcut and backtracking handling

Use Parcade for:

- Static solo/practice maps
- Lobby builds and historical schematics
- Existing commands and practice workflow
- Familiar plugin structure

## Checkpoint 1 — Mineplex leap reference in Parcade DE

- Port `PerkLeap`/`UtilAction.velocity` behavior into a small Parcade-owned helper.
- Preserve Dragon Escape Leaper parameters:
  - power: `1.0`
  - vertical addition: `0.2`
  - vertical cap: `1.0`
  - grounded boost: `0.2`
  - recharge: `8 seconds`
  - uses: `4`
- Preserve Parcade's ordinary Leap kit for side-by-side comparison.
- Avoid adding Mineplex Core or Arcade as runtime dependencies.
- Add deterministic vector tests and build for Java 8.
- Perform live A/B testing for any packet-level difference caused by Mineplex's custom velocity reapplication hook, which Parcade's server does not contain.

## Checkpoint 2 — DE Solo workflow

- Keep practice maps static instead of pasting every arena at startup.
- Add direct map selection and teleportation.
- Support fast reset/restart without rebuilding a shared arena world.
- Keep practice kits available, especially Mineplex Leap.
- Add speedrun timing independent of public-game state.
- Keep voting optional and disabled in solo mode.

## Checkpoint 3 — content and lobby

- Import a clean lobby from the archived `DE` world or lobby schematics.
- Convert selected historical Parcade maps into the preferred runtime format.
- Keep conversion tooling separate from production runtime dependencies.

## Checkpoint 4 — dragon and performance

Measure before rewriting:

- Tick duration
- Dragon movement and destruction time
- Blocks inspected versus changed per tick
- Loaded worlds and chunks
- Heap growth over repeated runs
- Scoreboard update cost

Likely Parcade optimization targets:

- Avoid pasting every configured arena at startup.
- Avoid one enormous arena world with maps spaced 2,500 blocks apart.
- Process only the newly entered edge of the dragon's destruction volume.
- Store compact block data instead of `String -> BlockState` entries where safe.
- Reduce scoreboard refresh frequency.
- Remove unused public matchmaking, MySQL, leaderboard, hologram, and production-network paths from solo deployments.

## Longer-term decision

After the leap and solo checkpoints, use measurements to decide whether to continue refactoring Parcade or extract a new focused Dragon Escape plugin. Do not make that decision before the first two checkpoints establish a working behavioral baseline.
