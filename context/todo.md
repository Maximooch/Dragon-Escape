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
- A kit-scoped adaptation of Mineplex's delayed velocity reapplication hook is now implemented; validate final outgoing packets and client trajectories before claiming exact parity.

## Mineplex Leap Parity

### Current baseline

- [x] Preserve the Mineplex Leaper launch constants, grounded boost, and four charges while keeping the ordinary Parcade Leap kit unchanged.
- [x] Fix the menu-name lookup falling back to None/Parkour: retain the compatible `Leapclassic` label with Mineplex lore.
- [x] Handle Spigot 1.8 right-click-air events despite their default cancelled state, without uncancelling vanilla actions.
- [x] Preserve intended velocity until the delayed `PlayerVelocityEvent`, following the local reference's `UtilAction` / `VelocityFix` path, with one-shot consumption and cleanup.
- [x] Pass vector tests and 31 isolated server checks, including kit lookup, axe delivery/reset, right-click dispatch, charges, cooldown blocking, usable-block exclusion, and delayed velocity restoration.
- User playtesting reports roughly 80–90% similarity. This is subjective feedback, not a measured parity percentage. Final packet and client trajectory equivalence remain unverified.

### Next investigations, in priority order

1. [ ] **Measure reference versus port velocity delivery and trajectories.**
   - Use `mineplex-reborn-dev` as the behavioral reference: `KitLeaper`, `PerkLeap`, `UtilAction`, `VelocityFix`, `UtilEnt`, and `UtilBlock`.
   - Run the same client with identical starting position, yaw/pitch, and movement state against both implementations.
   - Capture the activation vector, delayed velocity-event state, final outgoing velocity packet, and subsequent per-tick player positions.
   - Start with horizontal standing and airborne leaps; then test sprinting, downward aim, and steep upward aim.
   - Distinguish different launch packets from identical launches followed by different movement. Do not tune strength to compensate for an unmeasured timing problem.
2. [ ] **Match recharge timing precisely (confirmed discrepancy).**
   - Replace the Mineplex kit's use of Parcade's shared once-per-20-ticks countdown with timestamp-based recharge matching the reference's 8,000 ms interval.
   - Test activation near timer boundaries and under reduced TPS; preserve charges and solo reset behavior.
   - Keep the ordinary Parcade kit unchanged. This affects repeat-leap timing, not the trajectory of an individual leap.
3. [ ] **Verify grounded detection at collision boundaries.**
   - Compare full blocks, block edges, slabs, stairs, fences, and other partial-height blocks.
   - Test the tick immediately after jumping or walking off an edge.
   - Verify both the grounded decision and resulting 0.2 vertical boost against the reference rather than relying on visual similarity.
4. [ ] **Match surrounding movement conditions if launch packets already agree.**
   - Compare server builds/patches, sprint state, potion effects, client version/mods, and collision geometry.
   - Measure tick rate, latency, packet ordering, and other plugins changing velocity or teleporting players.
   - Treat these as hypotheses to test, not established defects.

### Acceptance criteria

- [ ] Define trajectory tolerances and the controlled test matrix before claiming full parity.
- [ ] Demonstrate matching outgoing launch packets and trajectories within those tolerances under identical conditions.
- [ ] Demonstrate matching cooldown, charge consumption, and reset behavior separately.
- [ ] Record reference-versus-port results and remaining runtime adaptations. Passing tests against our own implementation alone is not evidence of exact Mineplex equivalence.

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
