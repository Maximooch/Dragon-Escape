# Prototype asset pipeline

The source maps in `../schematics` are gzip-compressed legacy MCEdit
`.schematic` files (numeric pre-flattening block IDs). `SaltoFixed` is the
owned prototype environment: it removes stray entities, includes several map
fixes, and has explicit start-platform and finish-beacon landmarks.

## Repeatable browser pipeline

1. Convert the schematic to a surface mesh. For the prototype, run
   `tools/build_prototype_assets.py` with `nbtlib`, `numpy`, and `trimesh`.
   For Salto it keeps source-block resolution, removes internal voxel faces,
   preserves legacy block metadata, and emits GLB with explicit flat normals,
   vertex colors, and a neutral rough material. Face-direction shading and
   Minecraft-style per-corner ambient occlusion are baked in linear color space.
2. Compile solid voxels separately into greedy 3D collision cuboids and index
   them spatially at runtime. This preserves caves and overhangs; a top-down
   heightfield would incorrectly turn tree canopies into solid pillars.
3. Author characters and the dragon in Blender or Blockbench, export a single
   binary glTF (`.glb`), and preserve meaningful node names for animated parts.
4. Load GLB files through PlayCanvas container assets and instantiate render
   entities. Keep primitive fallbacks so a failed model request cannot prevent
   a race from starting.

The current Salto build is reproducible with:

```sh
python tools/build_prototype_assets.py ../schematics/SaltoFixed.schematic public/models \
  --factor 1 --asset-name salto --course-output lib/salto-course.generated.json \
  --scale 0.86 --start 147 296 --start-y 41 --finish 67 41
```

The converter packs axis-aligned normals with the standard
`KHR_mesh_quantization` extension. Asset tests require explicit normals and
materials so a loader fallback cannot silently discard the authored palette.

## Current visual limitations

- Salto is still one mesh/primitive, so its entire bounds are submitted even
  when only part of the route is visible.
- Blocks use a metadata-aware color palette, not a texture atlas. Stairs,
  slabs, fences, panes, plants, water, and glass are still cube approximations.
- Opaque, alpha-cutout, and translucent blocks are not split into distinct
  material passes.
- There is no authored sky/environment atlas or baked lightmap yet.

The next converter step should emit 32-cube chunks with a one-block sampling
halo, then greedy-merge only faces that share block material and four AO values.
For Salto, a measured 32-cube prototype produces 63 non-empty chunks and about
117k triangles before AO splits, versus 180k triangles in the current mesh.

## Blender-compatible alternatives

- Direct legacy route: `schem2obj` converts Minecraft 1.12 `.schematic` files
  to OBJ/MTL, which Blender imports directly.
- World route: load the schematic into a temporary WorldEdit/Amulet world,
  export the selected region with Mineways or jMc2Obj, then import OBJ in
  Blender.
- Web delivery: export from Blender as GLB. Use mesh compression only after
  verifying the target PlayCanvas build supports the chosen extension.

Release maps should be original and should keep render geometry, collision,
spawn/checkpoint data, and destruction groups as separate authored layers.
