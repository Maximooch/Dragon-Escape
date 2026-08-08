# Prototype asset pipeline

The source maps in `../schematics` are gzip-compressed legacy MCEdit
`.schematic` files (numeric pre-flattening block IDs). `GrumbleVolcano` is the
prototype environment because its route, floating landforms, and lava palette
fit Dragon Escape.

## Repeatable browser pipeline

1. Convert the schematic to a surface mesh. For the prototype, run
   `tools/build_prototype_assets.py` with `nbtlib`, `numpy`, and `trimesh`.
   It downsamples the scenery, removes internal voxel faces, and emits GLB.
2. Treat the GLB as visual scenery. Keep an authored, sparse collision course
   until a later converter emits navigation/collision metadata separately.
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
