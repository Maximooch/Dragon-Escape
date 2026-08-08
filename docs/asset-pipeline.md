# Prototype asset pipeline

The source maps in `../schematics` are gzip-compressed legacy MCEdit
`.schematic` files (numeric pre-flattening block IDs). `SaltoFixed` is the
owned prototype environment: it removes stray entities, includes several map
fixes, and has explicit start-platform and finish-beacon landmarks.

## Repeatable browser pipeline

1. Convert the schematic to a surface mesh. For the prototype, run
   `tools/build_prototype_assets.py` with `nbtlib`, `numpy`, and `trimesh`.
   For Salto it keeps source-block resolution, removes internal voxel faces,
   and emits GLB.
2. Compile solid voxels separately into greedy 3D collision cuboids and index
   them spatially at runtime. This preserves caves and overhangs; a top-down
   heightfield would incorrectly turn tree canopies into solid pillars.
3. Author characters and the dragon in Blender or Blockbench, export a single
   binary glTF (`.glb`), and preserve meaningful node names for animated parts.
4. Load GLB files through PlayCanvas container assets and instantiate render
   entities. Keep primitive fallbacks so a failed model request cannot prevent
   a race from starting.

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
