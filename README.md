# Dragon Escape MVP

A browser-first, first-person parkour race against an accelerating dragon. The route collapses behind the field as the dragon destroys authored breakable terrain.

Play the current public test at [dragon-escape-playtest.maximooch.chatgpt.site](https://dragon-escape-playtest.maximooch.chatgpt.site/).

## Included

- A selectable course catalog: authored **Salto** plus provisional imported test routes for **Caves**, **Frost Run**, and **Archipel**
- First-person mouse/touch look, keyboard or virtual-stick movement, jumping, sprinting, and an eight-second leap
- Countdown, checkpoints, course progress, placement, elimination, spectating, results, and rematch flow
- Voxel GLB environments, original MIDI scores synthesized through Web Audio, and authored dragon sound cues
- Offline demo rivals when a match server is unavailable
- Colyseus public-room server with server-side movement, collision, dragon, destruction, and match state
- Shared TypeScript simulation with automated rule tests
- Responsive desktop and mobile HUD, mobile safe-area handling, and a reduced mobile rendering budget

## Run locally

Requires Node.js 22.13 or newer.

```bash
npm install
npm run dev
```

In another terminal, start real public rooms:

```bash
npm run server
```

The web client uses `ws://localhost:2567` by default. Set `NEXT_PUBLIC_GAME_SERVER_URL` to the secure WebSocket endpoint for a deployed match server.

## Checks

```bash
npm test
npm run lint
npm run build
```

## Project map

- `app/` — game shell, HUD, menus, metadata
- `lib/client/` — PlayCanvas presentation and client prediction
- `lib/course.ts` — course catalog, route progress, geometry, and checkpoints
- `tools/course-specs/` — provisional waypoint metadata for imported test maps
- `tools/build_midi_tracks.py` — deterministic source for the original MIDI loops
- `lib/simulation.ts` — shared movement, collision, dragon and destruction rules
- `server/` — authoritative Colyseus public match room
- `tests/` — simulation and course invariants
