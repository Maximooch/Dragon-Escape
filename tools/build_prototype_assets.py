#!/usr/bin/env python3
"""Build lightweight GLB assets for the prototype.

Install tooling with: python -m pip install nbtlib numpy trimesh
The schematic output is intentionally downsampled and surface-only. It is a
visual reference mesh, not the authoritative gameplay collision map.
"""

from __future__ import annotations

import argparse
from pathlib import Path

import nbtlib
import numpy as np
import trimesh


def add_box(scene: trimesh.Scene, name: str, size, position, color) -> None:
    mesh = trimesh.creation.box(extents=size)
    mesh.apply_translation(position)
    mesh.visual.vertex_colors = np.tile(np.asarray(color, dtype=np.uint8), (len(mesh.vertices), 1))
    scene.add_geometry(mesh, node_name=name, geom_name=name)


def build_player(path: Path) -> None:
    scene = trimesh.Scene()
    skin = (174, 112, 72, 255)
    cloth = (28, 33, 43, 255)
    ember = (244, 68, 24, 255)
    boot = (12, 14, 20, 255)
    add_box(scene, "head", (0.62, 0.62, 0.62), (0, 1.72, 0), skin)
    add_box(scene, "hair", (0.66, 0.18, 0.66), (0, 2.00, -0.02), boot)
    add_box(scene, "torso", (0.72, 0.82, 0.38), (0, 1.02, 0), cloth)
    add_box(scene, "chest-mark", (0.32, 0.30, 0.035), (0, 1.10, -0.205), ember)
    add_box(scene, "arm-left", (0.25, 0.78, 0.25), (-0.50, 1.02, 0), skin)
    add_box(scene, "arm-right", (0.25, 0.78, 0.25), (0.50, 1.02, 0), skin)
    add_box(scene, "leg-left", (0.28, 0.75, 0.31), (-0.20, 0.25, 0), cloth)
    add_box(scene, "leg-right", (0.28, 0.75, 0.31), (0.20, 0.25, 0), cloth)
    add_box(scene, "boot-left", (0.31, 0.20, 0.43), (-0.20, -0.09, -0.05), boot)
    add_box(scene, "boot-right", (0.31, 0.20, 0.43), (0.20, -0.09, -0.05), boot)
    path.write_bytes(trimesh.exchange.gltf.export_glb(scene))


def build_dragon(path: Path) -> None:
    scene = trimesh.Scene()
    scale = (24, 21, 28, 255)
    ridge = (49, 43, 57, 255)
    ember = (255, 62, 8, 255)
    tooth = (226, 213, 185, 255)
    add_box(scene, "head", (3.4, 2.1, 4.2), (0, 0.2, 2.3), scale)
    add_box(scene, "brow", (3.7, 0.55, 1.5), (0, 1.15, 2.75), ridge)
    add_box(scene, "snout", (2.8, 1.0, 2.7), (0, -0.35, 4.8), scale)
    add_box(scene, "jaw", (2.55, 0.55, 2.5), (0, -1.03, 4.65), ridge)
    add_box(scene, "throat-fire", (1.9, 0.12, 1.8), (0, -0.72, 5.0), ember)
    for x in (-0.9, 0.9):
        add_box(scene, f"eye-{x}", (0.38, 0.34, 0.18), (x, 0.52, 4.46), ember)
    for x in (-0.72, -0.24, 0.24, 0.72):
        add_box(scene, f"tooth-{x}", (0.20, 0.34, 0.22), (x, -0.89, 6.0), tooth)
    add_box(scene, "neck", (2.8, 2.6, 3.4), (0, 0.2, -0.9), ridge)
    add_box(scene, "body", (4.7, 3.4, 6.2), (0, 0.2, -5.3), scale)
    add_box(scene, "left-wing", (9.5, 0.34, 5.5), (-6.8, 1.25, -5.6), ridge)
    add_box(scene, "right-wing", (9.5, 0.34, 5.5), (6.8, 1.25, -5.6), ridge)
    for i in range(4):
        add_box(scene, f"tail-{i}", (1.9 - i * 0.28, 1.8 - i * 0.24, 3.1), (0, 0.1, -9.3 - i * 2.65), scale)
    for z in (-1.8, -4.2, -6.6, -9.2):
        add_box(scene, f"spine-{z}", (0.45, 1.0, 0.65), (0, 2.15, z), ridge)
    path.write_bytes(trimesh.exchange.gltf.export_glb(scene))


def block_color(block_id: int) -> tuple[int, int, int, int]:
    if block_id in {10, 11, 51}:  # lava and fire
        return (255, 55, 5, 255)
    if block_id in {87, 88, 112}:  # netherrack, soul sand, nether brick
        return (71, 24, 27, 255)
    if block_id in {8, 9, 79}:
        return (20, 105, 138, 255)
    if block_id in {2, 31, 32, 37, 38, 106}:
        return (53, 103, 56, 255)
    if block_id in {12, 24}:
        return (154, 112, 70, 255)
    if block_id in {17, 5, 53, 85}:
        return (91, 57, 37, 255)
    if block_id in {78, 80}:
        return (205, 220, 226, 255)
    return (54, 49, 61, 255)


def build_schematic(source: Path, path: Path, factor: int) -> None:
    root = nbtlib.load(source, gzipped=True)
    width, height, length = (int(root[key]) for key in ("Width", "Height", "Length"))
    fine = np.frombuffer(bytes(root["Blocks"]), dtype=np.uint8).reshape((height, length, width))
    coarse_shape = tuple((value + factor - 1) // factor for value in fine.shape)
    padded = np.zeros(tuple(value * factor for value in coarse_shape), dtype=np.uint8)
    padded[:height, :length, :width] = fine
    grouped = padded.reshape(coarse_shape[0], factor, coarse_shape[1], factor, coarse_shape[2], factor)
    coarse = grouped.max(axis=(1, 3, 5))

    vertices: list[tuple[float, float, float]] = []
    faces: list[tuple[int, int, int]] = []
    colors: list[tuple[int, int, int, int]] = []
    directions = (
        ((1, 0, 0), ((1, 0, 0), (1, 1, 0), (1, 1, 1), (1, 0, 1))),
        ((-1, 0, 0), ((0, 0, 1), (0, 1, 1), (0, 1, 0), (0, 0, 0))),
        ((0, 1, 0), ((0, 1, 1), (1, 1, 1), (1, 1, 0), (0, 1, 0))),
        ((0, -1, 0), ((0, 0, 0), (1, 0, 0), (1, 0, 1), (0, 0, 1))),
        ((0, 0, 1), ((1, 0, 1), (1, 1, 1), (0, 1, 1), (0, 0, 1))),
        ((0, 0, -1), ((0, 0, 0), (0, 1, 0), (1, 1, 0), (1, 0, 0))),
    )
    occupied = np.argwhere(coarse != 0)
    ch, cl, cw = coarse.shape
    for y, z, x in occupied:
        color = block_color(int(coarse[y, z, x]))
        for (dx, dy, dz), corners in directions:
            nx, ny, nz = x + dx, y + dy, z + dz
            if 0 <= nx < cw and 0 <= ny < ch and 0 <= nz < cl and coarse[ny, nz, nx] != 0:
                continue
            base = len(vertices)
            for ox, oy, oz in corners:
                vertices.append((float(x + ox - cw / 2), float(y + oy), float(z + oz)))
                colors.append(color)
            faces.extend(((base, base + 1, base + 2), (base, base + 2, base + 3)))

    mesh = trimesh.Trimesh(vertices=np.asarray(vertices), faces=np.asarray(faces), process=False)
    mesh.visual.vertex_colors = np.asarray(colors, dtype=np.uint8)
    scene = trimesh.Scene(mesh)
    path.write_bytes(trimesh.exchange.gltf.export_glb(scene))
    print(f"{source.name}: {len(occupied):,} voxels, {len(faces):,} triangles -> {path}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("schematic", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--factor", type=int, default=3)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    build_player(args.output / "runner.glb")
    build_dragon(args.output / "cinder-wyrm.glb")
    build_schematic(args.schematic, args.output / "grumble-volcano.glb", args.factor)


if __name__ == "__main__":
    main()
