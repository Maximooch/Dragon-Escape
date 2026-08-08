# Dragon Escape product and engineering roadmap

## North star

Dragon Escape should become a browser-first, first-person parkour race that is understandable in one round and worth mastering for hundreds. Eight to twelve runners get a short head start, sprint through a collapsing voxel world, and improvise as a physical dragon destroys the route behind them. A round should take roughly three minutes; entering the next public race should take one click.

The release target is deliberately focused: one excellent public-race loop, five or six original maps, responsive Quake/Minecraft-inspired movement, a memorable dragon, reliable online play, and instant browser access. Parties, a level editor, alternate modes, and monetization are post-launch candidates rather than launch dependencies.

## Current baseline (Build 005)

The MVP already proves the broad loop:

- PlayCanvas renders a first-person browser game inside a React/Vinext shell.
- A shared TypeScript simulation implements movement, collision, checkpoints, an accelerating dragon, and destructible course blocks.
- A Colyseus room supports up to 12 runners at a 30 Hz server tick, with an offline bot fallback.
- SaltoFixed is converted at full resolution into a 180k-triangle GLB plus roughly 10k merged 3D collision cuboids, spatially indexed at runtime. Its owned schematic remains prototype/test content.
- Prototype GLBs exist for the runner and dragon, and the converter emits aligned environment and gameplay outputs from one source transform.
- A deterministic broadband audio check, dragon roar, and wing beat replace the nearly inaudible sub-bass-only mix; visible UI now reports ready/blocked/muted state.
- Salto now preserves legacy block metadata and renders with an explicit vertex-color material, flat normals, directional face shading, per-corner voxel AO, ACES tone mapping, and a warm-key/cool-fill/ember-rim lighting rig.
- Coarse-pointer devices now get a left movement stick, right-side drag look, jump/leap buttons, toggle sprint, safe-area-aware HUD, portrait guidance, and a mobile rendering budget with capped pixel ratio, shorter shadows, and fewer particles.
- The main menu now offers Salto plus provisional Caves, Frost Run, and Archipel test routes; each imported map has aligned GLB visuals, 3D collision, route metadata, and an original MIDI theme.
- Mouse capture, race HUD, results, rematch, and a small rule-test suite exist.

The main gaps are equally concrete:

- Sound is now physically confirmed in the originally affected browser; longer rematch/background-tab soak testing remains.
- Salto's route orientation, spawn, finish, scale, renderer, and full 3D collision now agree, but its provisional checkpoints, bot/dragon path, special block shapes, and intended line still require manual route QA.
- Course rules, collision, client presentation, audio, bots, and networking converge in one large runtime and a single global `COURSE` constant.
- Online reconciliation is prototype-grade. The match service, reconnect behavior, deployment, abuse controls, and load characteristics are not production-proven.
- There is no map manifest/compiler, animation system, settings screen, persistence, telemetry, compatibility matrix, or content-rights register.
- Release maps must be original. Imported Minecraft schematics are reference/test material unless ownership or a shipping license is recorded.

## Mobile roadmap

Mobile is now a supported browser surface for the MVP, not a later native-port concern. The immediate goal is a complete public race on current iOS Safari/Brave and Android Chrome without desktop controls, clipped UI, or accidental browser gestures.

**Current foundation**

- Coarse-pointer detection enables a virtual movement stick, drag-to-look region, held jump, leap, and toggle sprint.
- HUD and controls respect safe-area insets, recommend landscape, and use a reduced rendering budget.
- Low-height landscape layouts compact the header, race card, countdown, menu, results, and thumb controls independently of screen width.

**Near-term hardening (v0.2–v0.3)**

- Test a physical-device matrix covering iOS Safari, iOS embedded/alternative browsers, and Android Chrome at small and large phone sizes.
- Preserve input and camera state through orientation changes, address-bar expansion, app switching, and audio-context suspension.
- Add settings for look sensitivity, control handedness, HUD scale/opacity, sprint behavior, haptics, and reduced camera motion.
- Add installable PWA metadata and a user-initiated fullscreen path where the browser permits it; never require fullscreen to play.
- Keep touch targets at least 44 CSS px, reserve non-overlapping thumb zones, and prevent system back/home gestures from becoming game input where web APIs allow.

**Performance and release gates (v0.4+)**

- Define mobile GPU tiers with adaptive pixel ratio, shadow distance, particles, model LOD, texture compression, and streamed map chunks.
- Track first-playable time, frame-time percentiles, memory pressure, thermal throttling, battery drain, audio resumes, and orientation failures by device tier.
- Require a full race/rematch soak without control loss, clipped HUD, page zoom, or audio loss on the minimum supported iPhone and Android device.
- Add controller support only after the touch layout and accessibility settings are stable; evaluate a native wrapper only if browser retention justifies it.

## Product guardrails

1. **Fun before breadth.** Do not add progression or extra modes until repeat playtests say movement, route readability, and dragon pressure are fun.
2. **Browser first.** Current desktop and mobile browsers are the primary target. Native packaging may follow the web release, but touch controls and mobile performance are release requirements.
3. **Data, visuals, and collision stay separate.** A render GLB is not authoritative collision. Every map compiles to a small gameplay manifest plus optimized visual chunks.
4. **Public games stay frictionless.** Guest play and one-click rematches come before accounts. Accounts may enhance identity but must not gate the core loop.
5. **One fair simulation.** The server owns race outcomes. The client predicts movement and presents destruction but cannot decide position, cooldowns, or finish state.
6. **Rights are a build input.** A map or asset without recorded provenance cannot enter a release build.

## Version plan

Durations assume one primary developer using two or three bounded AI-agent workstreams and frequent human playtesting. They are sequencing estimates, not release promises. Original map design and art are the largest schedule variables.

### v0.2 — Build 003: a real imported test map (1–2 weeks)

**Outcome:** Salto becomes the first schematic-derived playable test course, while Ashen Causeway remains a fallback. Skylands may be evaluated afterward, but Salto is the safer first target because ownership has been explicitly stated.

**Work**

- Inspect schematic dimensions, palette, orientation, spawn area, route, and block density before converting.
- Extend the converter to emit:
  - optimized/chunked GLB scenery;
  - a gameplay manifest containing bounds, spawn, finish, checkpoints, death plane, collision boxes or merged collision mesh, and destructible groups;
  - a conversion report with source hash, block counts, unsupported IDs, output sizes, and rights status.
- Replace the global course singleton with a `CourseDefinition` selected by ID. Course selection may be a development URL flag at first.
- Derive sparse collision from route-relevant solids, not every visible voxel. Preserve a manual override layer for invisible barriers, spawn, checkpoints, and shortcuts.
- Align scale, axes, lighting, fog, dragon path, bot path, and HUD labels to the imported map.
- Add a course validator and a smoke test that loads every manifest without the renderer.

**Acceptance criteria**

- A player can start, traverse, reach every ordered checkpoint, finish, lose to the dragon, and rematch on Salto without editing code between rounds.
- Camera, collision, dragon travel, progress, destruction, and finish detection use the same coordinate system.
- There are no obvious holes, invisible walls on the intended line, or spawn-inside-geometry failures during five consecutive manual runs.
- The map visual payload is at most 12 MB uncompressed GLB and is split if a single draw/mesh budget is exceeded; a first profiling pass holds 60 fps at 1080p on a representative 2020-or-newer laptop.
- Conversion is repeatable from the original schematic with one documented command and produces the same manifest for the same source hash.
- Salto is labeled `prototype/testing` in its manifest; no ambiguous-rights schematic is included in a public release bundle.

**Dependencies:** legacy schematic parser, block-ID palette, confirmed Salto source file/version, and a decision about route orientation.

**Risks:** legacy metadata may not identify gameplay markers; dense geometry may overwhelm WebGL; visual landmarks may conceal an unclear parkour line. Mitigate with chunking, merged surfaces, authored marker overrides, and a simplified collision proxy.

**Good AI-agent splits:** schematic forensics/converter, course schema/tests, PlayCanvas integration/performance, and route QA can be separate branches. A human should choose the intended route and approve collision feel.

### v0.2.1 — Browser input and audio reliability (3–5 days, parallel with v0.2)

**Outcome:** every supported browser gives immediate, diagnosable feedback after the Join gesture and dragon audio remains audible through a whole round.

**Work**

- Replace ad hoc audio calls with an `AudioManager` that owns one context, master/music/SFX gains, lifecycle state, and source cleanup.
- Surface `locked`, `unlocking`, `running`, `suspended`, and `failed` states in a temporary diagnostics panel.
- Resume on explicit Join/Unmute gestures, handle `visibilitychange`/page resume, and avoid constructing secondary contexts inside growl/wing methods.
- Add a short, unmistakable audio-check sample or tone and an explicit retry affordance. Persist mute and volume settings.
- Add positional/distance attenuation only after reliable output is proven; then use authored or properly licensed dragon samples for final quality.
- Add pointer-lock sensitivity, invert-Y, field-of-view, and reduced-camera-bob settings.

**Acceptance criteria**

- On desktop Safari, Chrome, Firefox, and Edge, clicking Join or `Test sound` produces audible output within 250 ms with sound enabled.
- Countdown, dragon, UI, and results sounds survive tab background/foreground, pointer unlock/relock, rematch, and one reconnect.
- Mute is immediate, no source leaks across rematches, and no uncaught Web Audio errors appear in a ten-round soak.
- The UI distinguishes muted output from browser-blocked or failed output.
- Automated lifecycle tests cover context creation, suspension, resume, mute, and teardown; physical-browser listening remains a required manual gate.

**Dependencies:** access to affected browser/device and a reproducible failing session.

**Risks:** autoplay policies differ by browser and embedded context; synthesized low frequencies may be effectively inaudible on laptop speakers. Use a mid-frequency test cue and real-device QA rather than treating mocked `AudioContext` tests as proof.

**Good AI-agent splits:** lifecycle implementation/tests, cross-browser reproduction matrix, and sound-design integration. A human must confirm that sound is physically audible and the mix feels threatening rather than fatiguing.

### v0.3 — Movement and dragon vertical slice (2–3 weeks)

**Outcome:** one map demonstrates the release-quality feel and visual thesis from the voxel cover art.

**Work**

- Formalize movement parameters in a versioned config and build a small movement test arena.
- Add jump buffering, coyote time, predictable step/edge behavior, configurable air control, landing recovery, and unambiguous sprint/leap feedback.
- Record input/state replays so movement regressions can be reproduced deterministically.
- Replace rigid models with a Blockbench/Blender character rig and dragon rig: run, jump, fall, land, wing, turn, bite, and roar states.
- Add a readable dragon approach: silhouette, camera-safe screen shake, embers, debris, route destruction, spatial roar, and proximity mix.
- Add a brief playable onboarding run and clearer death/spectator transitions.
- Use the cover as a palette/lighting/material reference, while producing original game-ready assets.

**Acceptance criteria**

- The same recorded input produces equivalent checkpoint/finish results at 30, 60, and 120 render fps within documented tolerances.
- No common jump requires a single-frame input; a buffered jump and coyote jump each work in their specified windows.
- Ten new players can identify the route and controls without spoken help; at least eight complete the opening third in three attempts.
- Dragon distance is understandable from sight and sound before it kills the player.
- No animation can change authoritative collision or race outcome.
- Representative gameplay stays near 60 fps at 1080p, with a documented lower-quality preset for weaker integrated GPUs.

**Dependencies:** v0.2 course schema and v0.2.1 audio lifecycle.

**Risks:** adding “juice” can hide poor movement or create motion sickness. Keep camera effects optional, expose tuning values, and evaluate movement with effects disabled as well as enabled.

**Good AI-agent splits:** deterministic simulation/tests, animation state machine, VFX/performance, onboarding UX, and automated replay analysis. Human playtesting decides the final movement curve and threat pacing.

### v0.4 — Production multiplayer alpha (3–5 weeks)

**Outcome:** the public-room loop works reliably across real networks, not only locally or through demo rivals.

**Work**

- Define versioned network messages and separate authoritative state from render snapshots.
- Add input sequence numbers, acknowledgement, client prediction/replay, remote interpolation, bounded correction, and server-side movement validation.
- Implement room lifecycle: queue, countdown fill policy, late-join spectating, leave, reconnect grace period, results, and automatic next match.
- Deploy the Colyseus service behind secure WebSockets in a region near the initial audience. Add health checks, structured logs, crash reporting, metrics, and deploy rollback.
- Validate names, rate-limit messages, cap input values, and avoid trusting client cooldown, alive, checkpoint, or finish state.
- Build deterministic headless clients for latency, packet-loss, disconnect, and 12-player load tests.

**Acceptance criteria**

- Twelve simulated clients can complete 100 consecutive room cycles without a crash, stuck phase, leaked room, or unbounded memory growth.
- At 100 ms round-trip latency and 2% packet loss, local movement remains responsive, remote runners remain legible, and corrections do not routinely exceed 0.5 m.
- Reconnecting within five seconds restores the correct runner or spectator state; joining mid-race cannot enter as a live competitor.
- Server tick p95 remains under 20 ms at the planned per-instance room count, with an alert before saturation.
- A manipulated client cannot exceed configured speed/leap limits or report its own finish.
- Browser and game-service versions fail gracefully when their protocol versions differ.

**Dependencies:** stable movement model, course manifests available on client and server, hosting choice, and observability destination.

**Risks:** simulation drift, hosting cost, regional latency, cheating, and empty lobbies. Mitigate with input-driven authority, load tests, one launch region, clear capacity metrics, and bots only as explicitly labeled backfill.

**Good AI-agent splits:** protocol/reconciliation, room lifecycle, deployment/observability, adversarial validation, and headless load clients. Integration and live latency tests require a single owner.

### v0.5 — Original-content pipeline and three-map alpha (4–6 weeks, overlaps v0.4)

**Outcome:** maps are original, cheap to iterate, validated automatically, and consistent enough for competitive public rotation.

**Work**

- Turn the prototype converter into a general map compiler with a stable manifest schema and schema migrations.
- Adopt a Blender/Blockbench authoring contract for named spawn, checkpoint, finish, kill, collision, decoration, bot-route, dragon-route, and destruction nodes.
- Add map validation for route ordering, bounds, unsupported materials, missing markers, collision complexity, asset size, and duplicate IDs.
- Build three original maps: an accessible launch course, a vertical/intermediate course, and a technical/expert course. Imported schematics may inform conversion tests but not final geometry.
- Add map rotation, map-specific best times, difficulty labels, previews, and post-race map voting only if voting does not slow the rematch loop.
- Establish reusable voxel material, prop, VFX, lighting, and audio libraries.

**Acceptance criteria**

- A designer can change geometry/markers, export, compile, validate, and play locally without modifying TypeScript.
- Every map has at least two viable lines, one readable recovery opportunity, no unintended skip that removes more than 15% of the route, and a complete bot/replay proof.
- All three maps meet agreed draw-call, triangle, texture-memory, collision, and compressed-download budgets.
- Five consecutive public rotations load the correct map on every client and server and produce consistent checkpoints/results.
- A rights manifest records author, source files, license/ownership, and third-party dependencies for every shipped asset.

**Dependencies:** stable course schema, art-direction guide, original level-design capacity, and performance budgets measured in v0.2/v0.3.

**Risks:** content creation dominates schedule; automated conversion cannot create good parkour; map variety can fragment balance. Use modular kits, graybox before art, weekly route tests, and a small launch map count with high replayability.

**Good AI-agent splits:** validator/compiler, Blender export helpers, asset optimization, automated route/skip checks, metadata/rights audit, and per-map regression suites. Humans own level design, visual composition, and rights decisions.

### v0.6 — Closed alpha game shell (3–4 weeks)

**Outcome:** the game is coherent for invited players who arrive without developer guidance.

**Work**

- Add a first-run tutorial, settings, key rebinding, sensitivity/FOV controls, audio sliders, quality presets, and accessible HUD scaling/contrast.
- Improve lobby, countdown, placement, elimination, spectating, results, rematch, loading, offline, incompatible-version, and server-error states.
- Add local guest identity, optional display-name moderation, and privacy-preserving session identifiers.
- Add telemetry for load time, join outcome, disconnect, fps tier, completion, death location, rematch, and map selection. Do not capture raw input or unnecessary personal data.
- Add feedback capture linked to build/map/browser metadata.
- Run weekly instrumented playtests and maintain a ranked issue backlog.

**Acceptance criteria**

- A first-time player can load, understand controls, join, race, spectate, read results, change settings, and rematch without external instruction.
- All failure states offer a next action; a game-service outage falls back or explains itself rather than appearing to hang.
- Keyboard-only menus work, focus is visible, color is not the sole carrier of information, reduced motion is respected, and essential text remains legible at 200% browser zoom.
- Telemetry can answer funnel, crash/disconnect, map completion, death-cluster, and rematch questions by build without recording sensitive content.
- Closed-alpha sessions achieve at least 90% successful joins and 99% crash-free game sessions before expanding access.

**Dependencies:** production-like multiplayer, privacy decisions, and a stable enough game loop for metrics to be meaningful.

**Risks:** dashboards and accounts can consume time without improving fun. Instrument only decisions the team is prepared to make, and keep guest play as the default.

**Good AI-agent splits:** accessibility/settings, failure-state UX, telemetry schema/dashboard, moderation tests, and playtest report synthesis. Human review is required for privacy language and interpreting player behavior.

### v0.7 — Public beta: retention without grind (4–6 weeks)

**Outcome:** players have reasons to improve and return while the one-click public-race loop remains intact.

**Work**

- Add local and optional account-backed personal bests, map medals, run history, and ghost replay.
- Add server-verified leaderboards only after replay validation and anti-cheat rules are in place.
- Introduce cosmetic-only runner/dragon-trail unlocks, lightweight challenges, and a fair map rotation. No power progression.
- Expand to five original maps after graybox and balance gates pass.
- Improve matchmaking for low population: regional threshold, room fill window, transparent bots, and immediate next-race continuity.
- Run browser/device compatibility, latency, accessibility, and capacity beta cohorts.

**Acceptance criteria**

- Personal bests and ghosts are map-versioned; changing a course cannot silently compare incompatible times.
- Leaderboard submissions are backed by a server-authoritative result and replay/input evidence sufficient for audit.
- Cosmetics never change collision silhouette, camera visibility, speed, jump, leap, or route readability.
- At least 95% of supported-browser sessions reach the menu, 92% of join attempts reach a race, and crash-free sessions exceed 99.5% during the beta window.
- The median returning tester plays at least three rounds per session, while first-race abandonment and death clusters are reviewed per map.

**Dependencies:** accounts/persistence choice, authoritative results, replay format, original content, and credible beta population.

**Risks:** leaderboards attract cheating; progression can distort the clean premise; low concurrency can make matchmaking look broken. Defer global rankings if validation is weak and favor personal mastery over grind.

**Good AI-agent splits:** replay/ghost tooling, persistence migrations, leaderboard abuse tests, cosmetic pipeline, cohort analysis, and browser automation. Human product judgment chooses which retention features deserve to ship.

### v0.8 — Content-complete beta (3–5 weeks)

**Outcome:** the intended 1.0 feature set and five-to-six-map lineup are complete; work shifts from invention to polish and proof.

**Work**

- Finish the launch maps, dragon/runner animations, music, soundscape, VFX, menus, credits, and legal notices.
- Lock network protocol and map schema except for backward-compatible fixes.
- Complete adaptive quality settings, asset streaming/chunking, caching, and loading transitions.
- Balance dragon acceleration, destruction cadence, abilities, shortcuts, checkpoint rules, and round timing across the full rotation.
- Conduct structured usability, motion-sensitivity, color-vision, controller-feasibility, and laptop-speaker audio tests.

**Acceptance criteria**

- No placeholder or ambiguous-rights asset remains in a release bundle.
- Every launch map passes a 50-round soak, route/skip review, collision review, and performance budget on low and recommended hardware tiers.
- First playable content appears within eight seconds on a 25 Mbps warm-region connection and subsequent map transitions stay under five seconds at p75.
- Recommended hardware holds 60 fps at 1080p in a 12-runner worst case; the low preset holds 30 fps on the defined minimum tier.
- Voice/SFX/music levels remain intelligible on laptop speakers and headphones; settings persist across sessions.
- Feature, content, and string freeze begins when all criteria pass.

**Dependencies:** all preceding systems and final original assets.

**Risks:** late art raises memory/download cost; schema churn invalidates maps; polish work grows without a stopping rule. Enforce asset budgets in CI and require explicit approval for post-freeze features.

**Good AI-agent splits:** asset audit/optimization, map soak automation, balance-data analysis, compatibility runs, credits/license audit, and regression expansion. Final creative and accessibility sign-off stays human-led.

### v0.9 — Release candidate (3–4 weeks)

**Outcome:** a releasable build has survived operational, security, compatibility, and recovery exercises.

**Work**

- Freeze features and run full regression, browser/device, accessibility, load, soak, reconnect, upgrade, rollback, and backup/restore tests.
- Threat-model the web client, APIs, room server, persistence, admin tools, names, and leaderboard path; remediate high-severity findings.
- Add release channels, canary deployment, feature flags/kill switches, status communication, incident runbooks, dashboards, alerts, and cost ceilings.
- Finalize terms/privacy, content rights, age/audience decisions, moderation policy, support path, credits, and marketing captures.
- Rehearse launch-day scale and an emergency rollback to the last known-good client/server protocol pair.

**Acceptance criteria**

- No open blocker/critical defect, no known high-severity security finding, and every lower-severity deferral has an owner and rationale.
- A seven-day release-candidate soak meets 99.5% crash-free sessions, 99.9% game-service availability, successful-join and server-tick targets, and the agreed error budget.
- Canary, rollback, protocol mismatch, service outage, and database recovery drills succeed from written runbooks.
- Supported browsers pass the complete race/rematch flow with sound, pointer lock, and settings; unsupported environments get a useful message.
- Rights, privacy, credits, and release-map originality are signed off.

**Dependencies:** content freeze, representative traffic/load model, and operational ownership.

**Risks:** browser regressions and infrastructure failures can appear only under real traffic. Use staged rollout, observable versioning, conservative capacity, and reversible data changes.

**Good AI-agent splits:** regression triage, security review support, load/soak analysis, release-note generation, rights inventory cross-check, and runbook validation. Deploy/rollback authority and launch go/no-go remain human decisions.

### v1.0 — Browser launch

**Launch scope**

- One polished 8–12-player public Dragon Escape mode.
- Five or six original maps in a fast automatic rotation.
- Guest play, strong first-person movement, leap ability, spectating, rematches, personal mastery/ghosts, and optional cosmetic identity.
- Server-authoritative races with reconnect, abuse controls, telemetry, and operational rollback.
- Original voxel-gothic presentation, animated dragon/runner, complete audio, settings, quality tiers, and baseline accessibility.

**Launch gate:** v0.9 criteria remain healthy through staged rollout, support and incident ownership are staffed, and no launch feature depends on an unverified third-party asset or service.

## Post-launch possibilities

These are deliberately outside the 1.0 critical path:

- **v1.1:** one new map, time-limited map modifiers, improved ghosts, spectating polish, and balance changes driven by launch data.
- **v1.2:** parties/private rooms, friend invites, community tournaments, controller support, and installable PWA/native desktop wrapper.
- **v1.3:** a constrained original-map workshop with validation, moderation, versioning, and featured rotations. Do not expose raw schematic uploads to public play.
- **Later:** alternate dragon behaviors, cooperative relay or endless escape variants, seasonal visual themes, creator tools, and Steam/itch distribution if browser retention justifies them.

## Critical path and parallel work

The critical path is:

`course schema/compiler → playable imported test map → movement/threat vertical slice → authoritative multiplayer → original map pipeline/content → beta proof → release candidate`

Audio reliability, animation/VFX, UI/accessibility, telemetry, and asset-rights work can run alongside portions of that path. They still need scheduled integration points; parallel branches that all modify `DragonEscapeRuntime.ts` will create conflicts until the runtime is split into focused systems.

Before v0.4, extract at least these boundaries:

- `GameSession` — phase, race, result, and reset lifecycle;
- `RunnerController` — local input, prediction, camera, and movement presentation;
- `CourseRuntime` — manifest loading, collision, checkpoints, and destruction presentation;
- `NetworkClient` — protocol, snapshots, reconciliation, and reconnect;
- `AudioManager` — browser lifecycle, buses, spatial sources, and settings;
- `ActorView` — runner/dragon model, animation, interpolation, and VFX;
- `GameViewModel` — throttled UI state independent of PlayCanvas entities.

AI agents are most effective when each gets one boundary, explicit acceptance tests, and ownership of a non-overlapping file set. A release/integration agent should keep the build green, rebase small changes, and reject work that lacks proof. Agents can accelerate conversion, schema work, tests, compatibility scripts, performance reports, and asset audits; they cannot replace physical-browser audio checks, multiplayer feel tests, art direction, map design, rights confirmation, privacy decisions, or launch authority.

## Permanent quality gates

Every version should keep these gates green:

- Unit tests for simulation, course validation, audio lifecycle, and input mapping.
- Deterministic replay tests for movement, abilities, checkpoints, dragon catch, and finish ordering.
- Client/server protocol compatibility tests and headless room-cycle tests.
- GLB/manifest validation, asset-size budgets, and a provenance record for release content.
- Lint, type/build, dependency audit, and `git diff --check` in CI.
- Automated browser smoke tests for load, Join, pointer lock prompt, race, result, settings, and rematch; manual sound verification remains separate.
- A performance capture on minimum and recommended hardware for every content-complete candidate.
- Structured playtest notes tied to build, map version, browser, hardware tier, and latency.

## Top risks to review every milestone

| Risk | Early warning | Mitigation / decision trigger |
| --- | --- | --- |
| Movement is technically sound but not fun | Low rematch rate; players blame controls | Stop feature work, run movement-only tests, tune from replays and observation |
| Imported content creates legal exposure | Unknown author/source or conflicting provenance | Keep it out of release bundles; replace with original graybox immediately |
| Map conversion produces beautiful but bad courses | Route confusion, invisible collision, severe skips | Treat conversion as scaffolding; author route/collision/markers separately |
| Web performance collapses as art improves | Long first load, GPU memory growth, frame spikes | Enforce per-map budgets in CI; chunk/LOD/instance before adding more assets |
| Audio “passes tests” but remains silent | Context says running while testers hear nothing | Add observable diagnostics and a mid-frequency test cue; require physical checks |
| Client/server simulation diverges | Frequent correction pops or disputed finishes | Input acknowledgement/replay, versioned config, server-owned outcomes, latency tests |
| Empty public rooms damage first impression | Long waits or repeated solo starts | One region/queue initially, short fill window, labeled bots, immediate rotation |
| Scope expands into accounts/editor/modes too soon | Core completion and rematch metrics remain weak | Hold guardrails; require a milestone gate before opening a new product surface |
| AI parallelism increases integration debt | Large overlapping diffs and flaky main | Narrow tasks, shared contracts first, one integrator, small merges, mandatory proof |

## Realistic reach

With disciplined scope, the current prototype can become a strong browser indie release in roughly 24–36 focused development weeks plus original art/map production. AI agents can compress implementation and QA work, but the calendar will still be governed by repeated human playtests, cross-browser proof, network soak time, and creation of truly original maps. The fastest credible route is not to add more systems now: make Salto playable, fix audio with an observable lifecycle, decide whether movement and dragon pressure are genuinely fun, and only then invest in production multiplayer and content scale.
