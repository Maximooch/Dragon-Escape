# Dragon Escape engineering references

This is the working technical compass for the browser game. The goal is not to
turn Dragon Escape into a Quake port or a general voxel engine; it is to borrow
proven ideas that directly improve first-person feel, repeatability, and static
course rendering.

## Quake as a movement reference

The most useful source is Quake III's shared player-movement code:

- [`bg_pmove.c`](https://github.com/id-Software/Quake-III-Arena/blob/master/code/game/bg_pmove.c)
  separates input into a normalized desired direction and desired speed,
  applies ground friction separately, and accelerates only along that desired
  direction. This is the heart of responsive strafing without a free diagonal
  speed boost.
- [`bg_slidemove.c`](https://github.com/id-Software/Quake-III-Arena/blob/master/code/game/bg_slidemove.c)
  sweeps movement through remaining frame time, clips velocity against contact
  planes, moves along a two-plane crease, and attempts a step-slide. These are
  better long-term parkour primitives than cancelling an entire movement axis
  after overlap.
- [`cg_predict.c`](https://github.com/id-Software/Quake-III-Arena/blob/master/code/cgame/cg_predict.c)
  is the architectural anchor: the client predicts the same command-driven
  movement that the server verifies, then replays unacknowledged commands after
  a correction.

For readable explanations, see [Adrian Biagioli on bunnyhopping](https://adrianb.io/2015/02/14/bunnyhop.html),
[Fabien Sanglard's Quake III architecture notes](https://www.fabiensanglard.net/quake3/index.php),
and his [prediction walkthrough](https://fabiensanglard.net/quakeSource/quakeSourcePrediction.php).

Dragon Escape should independently implement those concepts with its own code
and tests. The [id source release](https://github.com/id-Software/Quake-III-Arena)
is GPL-2.0-or-later, and its source release does not grant rights to Quake maps,
models, textures, or sounds. Copying or porting GPL implementation code would
need a deliberate licensing decision; studying behavior and reimplementing the
mechanics is the intended path here.

### Concrete movement target

1. Run movement at a fixed 120 Hz and interpolate presentation. Glenn Fiedler's
   [Fix Your Timestep](https://gafferongames.com/post/fix_your_timestep/)
   explains the accumulator, interpolation, and catch-up limit.
2. Replace target-velocity interpolation with wish-direction acceleration,
   distinct ground/air acceleration, and explicit friction.
3. Add swept capsule/box collision, plane clipping, and a conservative step
   height around 0.45–0.52 world units for the current 0.86-unit blocks.
4. Add roughly 80 ms coyote time and 100 ms jump buffering. Preserve air
   strafing, but initially cap horizontal speed around 1.25–1.4 times sprint so
   a mastered movement exploit cannot erase the authored route.
5. Record inputs and assert equivalent results under 60, 120, and 144 Hz render
   schedules before using the same simulation for server reconciliation.

## Quake as a world-pipeline reference

The important rendering lesson is offline compilation, not BSP itself:

```text
source map -> optimized visual chunks + collision + baked lighting metadata
           -> cheap runtime culling, simulation, and presentation
```

Quake III compiled visibility, surfaces, and lightmaps ahead of time; Sanglard's
[renderer analysis](https://www.fabiensanglard.net/quake3/renderer.php) is a
useful tour. Salto's open floating islands do not offer the enclosed rooms that
make portal/PVS systems shine. For this game, chunk frustum culling plus the
existing collision grid is the simpler and more relevant equivalent.

## Voxel rendering references

- Mikola Lysenko's [voxel meshing article](https://0fps.net/2012/06/30/meshing-in-a-minecraft-game/)
  gives the progression from naive cubes to hidden-face culling and greedy
  quads. Dragon Escape now culls hidden faces; metadata/AO-aware greedy chunks
  are the next step.
- His [voxel ambient-occlusion article](https://0fps.net/2013/07/03/ambient-occlusion-for-minecraft-like-worlds/)
  derives the three-neighbor corner rule and explains how to choose a quad's
  diagonal. Build 004 applies that rule in the converter.
- Vercidium's [Sector's Edge optimization write-up](https://vercidium.com/blog/voxel-world-optimisations/)
  is a practical implementation reference for 32-cube chunks and face-run
  merging.
- PlayCanvas's [batching guidance](https://developer.playcanvas.com/user-manual/graphics/advanced-rendering/batching/)
  explains the opposing costs: fewer draw calls versus larger bounds that
  cannot be culled. Salto measures at 63 non-empty 32-cube chunks, a sensible
  first budget to profile.

The next map-renderer slice should be:

1. Emit 32-cube render chunks with a one-block halo for correct edge culling
   and AO. A measured `(block id, metadata)` greedy pass reduces Salto from
   about 180k to 117k triangles before AO boundaries.
2. Split opaque, alpha-cutout, and translucent geometry.
3. Add one original or clearly licensed nearest-filtered texture atlas, with
   metadata-aware top/side/bottom faces.
4. Generate actual slab, stair, fence, pane, ladder, vine, plant, water, and
   glass shapes instead of full-cube proxies.
5. Keep visual chunks separate from collision, checkpoints, bot/dragon routes,
   and destruction groups.

## Lighting and color references

Build 004 uses a deliberately modest real-time rig: ambient base, shadowed warm
key, cool fill, ember rim, local lava/beacon lights, explicit sRGB vertex-color
handling, and ACES tone mapping. The lava intensity is now below clipping so
ACES can preserve an orange-red gradient rather than producing a flat red plane.

Relevant PlayCanvas references are the
[StandardMaterial API](https://api.playcanvas.com/engine/classes/StandardMaterial.html),
[linear workflow](https://developer.playcanvas.com/user-manual/graphics/linear-workflow/),
[tone mapping and exposure](https://developer.playcanvas.com/user-manual/graphics/cameras/tone-mapping/),
and [shadow guidance](https://developer.playcanvas.com/user-manual/graphics/lighting/shadows/).
Once chunking and materials settle, evaluate an original environment atlas or
baked static lighting through PlayCanvas
[image-based lighting](https://developer.playcanvas.com/user-manual/graphics/physical-rendering/image-based-lighting/)
and [lightmapping](https://developer.playcanvas.com/user-manual/graphics/lighting/lightmapping/).

## What not to build yet

- No custom BSP/PVS renderer for open floating-island courses.
- No full rigid-body physics replacement solely to imitate Quake movement.
- No texture-atlas polishing before chunk bounds and special block geometry are
  correct.
- No copied Minecraft or Quake art/audio assets; both technical prototypes and
  release content still need explicit provenance.

PlayCanvas plus the pure TypeScript simulation remains the reliable stack. If
swept capsule collision, slopes, or moving platforms eventually outgrow the
small controller, [Rapier's JavaScript character controller](https://rapier.rs/docs/user_guides/javascript/character_controller/)
is the fallback to prototype rather than a dependency to add preemptively.
