#!/usr/bin/env python3
"""Build lightweight GLB assets for the prototype.

Install tooling with: python -m pip install nbtlib numpy trimesh
The schematic output is surface-only and can optionally be downsampled. It is
a visual reference mesh, not the authoritative gameplay collision map.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import struct

import nbtlib
import numpy as np
import trimesh


GLB_JSON_CHUNK = 0x4E4F534A
GLB_BIN_CHUNK = 0x004E4942


def export_glb_with_material(scene: trimesh.Scene, material_name: str, *, quantize_normals: bool = False) -> bytes:
    """Export a GLB with an explicit neutral, rough material.

    Trimesh deliberately omits a material for color-only meshes. Vertex colors
    are still valid glTF in that case, but an explicit material keeps other
    importers from choosing their own gloss/metalness defaults.
    """
    payload = trimesh.exchange.gltf.export_glb(scene)
    magic, version, total_length = struct.unpack_from("<4sII", payload, 0)
    if magic != b"glTF" or version != 2 or total_length != len(payload):
        raise ValueError("trimesh returned an invalid GLB")

    offset = 12
    chunks: list[tuple[int, bytes]] = []
    while offset < total_length:
        length, chunk_type = struct.unpack_from("<II", payload, offset)
        offset += 8
        chunks.append((chunk_type, payload[offset:offset + length]))
        offset += length

    document = json.loads(chunks[0][1].decode("utf-8").rstrip("\x00 "))
    document["materials"] = [{
        "name": material_name,
        "pbrMetallicRoughness": {
            "baseColorFactor": [1.0, 1.0, 1.0, 1.0],
            "metallicFactor": 0.0,
            "roughnessFactor": 0.92,
        },
    }]
    for mesh in document.get("meshes", []):
        for primitive in mesh.get("primitives", []):
            primitive["material"] = 0

    if quantize_normals:
        # A full float normal costs 12 bytes per vertex. Axis-aligned voxel
        # normals are losslessly represented for rendering as normalized signed
        # bytes, reducing Salto by roughly 3 MB. glTF requires the standard
        # KHR_mesh_quantization declaration for this NORMAL representation.
        primitive = document["meshes"][0]["primitives"][0]
        normal_accessor_index = primitive["attributes"]["NORMAL"]
        normal_accessor = document["accessors"][normal_accessor_index]
        normal_view_index = normal_accessor["bufferView"]
        normal_view = document["bufferViews"][normal_view_index]
        bin_chunk_index = next(index for index, (kind, _) in enumerate(chunks) if kind == GLB_BIN_CHUNK)
        binary = chunks[bin_chunk_index][1]
        start = normal_view.get("byteOffset", 0) + normal_accessor.get("byteOffset", 0)
        old_length = normal_view["byteLength"]
        count = normal_accessor["count"]
        source_normals = np.frombuffer(binary, dtype="<f4", count=count * 3, offset=start).reshape(count, 3)
        packed_normals = np.zeros((count, 4), dtype=np.int8)
        packed_normals[:, :3] = np.rint(np.clip(source_normals, -1.0, 1.0) * 127).astype(np.int8)
        replacement = packed_normals.tobytes()
        old_end = normal_view.get("byteOffset", 0) + old_length
        new_binary = binary[:normal_view.get("byteOffset", 0)] + replacement + binary[old_end:]
        delta = len(replacement) - old_length
        for index, view in enumerate(document["bufferViews"]):
            if index != normal_view_index and view.get("byteOffset", 0) >= old_end:
                view["byteOffset"] = view.get("byteOffset", 0) + delta
        normal_view["byteLength"] = len(replacement)
        normal_view["byteStride"] = 4
        normal_accessor["componentType"] = 5120  # signed byte
        normal_accessor["normalized"] = True
        normal_accessor.pop("max", None)
        normal_accessor.pop("min", None)
        document["buffers"][normal_view["buffer"]]["byteLength"] += delta
        document.setdefault("extensionsUsed", []).append("KHR_mesh_quantization")
        document.setdefault("extensionsRequired", []).append("KHR_mesh_quantization")
        chunks[bin_chunk_index] = (GLB_BIN_CHUNK, new_binary)

    json_payload = json.dumps(document, separators=(",", ":")).encode("utf-8")
    json_payload += b" " * ((-len(json_payload)) % 4)
    chunks[0] = (GLB_JSON_CHUNK, json_payload)
    output_length = 12 + sum(8 + len(data) for _, data in chunks)
    output = bytearray(struct.pack("<4sII", magic, version, output_length))
    for chunk_type, data in chunks:
        output.extend(struct.pack("<II", len(data), chunk_type))
        output.extend(data)
    return bytes(output)


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
    path.write_bytes(export_glb_with_material(scene, "Runner voxel character"))


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
    path.write_bytes(export_glb_with_material(scene, "Cinder Wyrm voxel character"))


DYE_PALETTE = (
    (221, 224, 222, 255), (208, 99, 39, 255), (173, 73, 180, 255), (77, 142, 174, 255),
    (221, 179, 50, 255), (106, 174, 54, 255), (211, 112, 143, 255), (67, 68, 76, 255),
    (143, 148, 151, 255), (42, 127, 142, 255), (113, 65, 151, 255), (49, 67, 142, 255),
    (109, 72, 50, 255), (58, 102, 58, 255), (178, 55, 48, 255), (28, 30, 38, 255),
)


def shade_srgb(color: tuple[int, int, int, int], shade: float) -> tuple[int, int, int, int]:
    """Apply lighting in linear space while storing glTF vertex colors as sRGB."""
    output = []
    for byte in color[:3]:
        encoded = byte / 255.0
        linear = encoded / 12.92 if encoded <= 0.04045 else ((encoded + 0.055) / 1.055) ** 2.4
        linear = min(1.0, max(0.0, linear * shade))
        encoded = linear * 12.92 if linear <= 0.0031308 else 1.055 * linear ** (1 / 2.4) - 0.055
        output.append(round(encoded * 255))
    return tuple(output) + (color[3],)


def block_color(block_id: int, data: int) -> tuple[int, int, int, int]:
    if block_id in {35, 95, 159, 160, 171}:
        return DYE_PALETTE[data & 15]
    if block_id in {10, 11, 51}:  # lava and fire
        return (255, 55, 5, 255)
    if block_id in {87, 88, 112}:  # netherrack, soul sand, nether brick
        return (94, 31, 34, 255)
    if block_id in {8, 9, 79}:
        return (28, 121, 159, 255)
    if block_id in {2, 31, 32, 37, 38, 106}:
        return (51, 126, 78, 255)
    if block_id == 18:
        return (39, 113, 77, 255)
    if block_id in {1, 4, 43, 44, 67, 98, 109, 139}:
        return (91, 93, 116, 255)
    if block_id in {3, 60}:
        return (102, 72, 56, 255)
    if block_id in {12, 24}:
        return (177, 133, 77, 255)
    if block_id in {17, 5, 53, 85, 188}:
        return (122, 78, 50, 255)
    if block_id == 82:
        return (114, 129, 143, 255)
    if block_id in {78, 80}:
        return (211, 226, 232, 255)
    if block_id in {41, 42, 57, 133, 138}:
        return (72, 211, 203, 255)
    if block_id in {16, 173}:
        return (34, 35, 46, 255)
    return (82, 78, 99, 255)


def load_coarse_schematic(source: Path, factor: int) -> np.ndarray:
    root = nbtlib.load(source, gzipped=True)
    width, height, length = (int(root[key]) for key in ("Width", "Height", "Length"))
    block_ids = np.frombuffer(bytes(root["Blocks"]), dtype=np.uint8).reshape((height, length, width)).astype(np.uint16)
    block_data = np.frombuffer(bytes(root["Data"]), dtype=np.uint8).reshape((height, length, width)).astype(np.uint16) & 15
    fine = block_ids * 16 + block_data
    coarse_shape = tuple((value + factor - 1) // factor for value in fine.shape)
    padded = np.zeros(tuple(value * factor for value in coarse_shape), dtype=np.uint16)
    padded[:height, :length, :width] = fine
    grouped = padded.reshape(coarse_shape[0], factor, coarse_shape[1], factor, coarse_shape[2], factor)
    return grouped.max(axis=(1, 3, 5))


def build_schematic(source: Path, path: Path, factor: int, material_name: str = "Voxel terrain") -> np.ndarray:
    coarse = load_coarse_schematic(source, factor)

    vertices: list[tuple[float, float, float]] = []
    normals: list[tuple[float, float, float]] = []
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
    block_ids = coarse >> 4
    # 166 is Minecraft's invisible barrier block. Several archived maps use it
    # for editor guides and safety volumes; rendering it creates giant magenta
    # diamonds and planes that were never visible in the original game.
    occupied = np.argwhere((block_ids != 0) & (block_ids != 166))
    ch, cl, cw = coarse.shape

    def occupied_at(x: int, y: int, z: int) -> bool:
        return 0 <= x < cw and 0 <= y < ch and 0 <= z < cl and block_ids[y, z, x] not in {0, 166}

    face_shades = {
        (1, 0, 0): 0.84,
        (-1, 0, 0): 0.72,
        (0, 1, 0): 1.15,
        (0, -1, 0): 0.52,
        (0, 0, 1): 0.78,
        (0, 0, -1): 0.94,
    }
    for y, z, x in occupied:
        packed = int(coarse[y, z, x])
        color = block_color(packed >> 4, packed & 15)
        for (dx, dy, dz), corners in directions:
            nx, ny, nz = x + dx, y + dy, z + dz
            if occupied_at(nx, ny, nz):
                continue
            base = len(vertices)
            ao_levels: list[int] = []
            for ox, oy, oz in corners:
                vertices.append((float(x + ox - cw / 2), float(y + oy), float(z + oz)))
                normals.append((float(dx), float(dy), float(dz)))
                outside = [x + dx, y + dy, z + dz]
                if dx:
                    axes = ((1, -1 if oy == 0 else 1), (2, -1 if oz == 0 else 1))
                elif dy:
                    axes = ((0, -1 if ox == 0 else 1), (2, -1 if oz == 0 else 1))
                else:
                    axes = ((0, -1 if ox == 0 else 1), (1, -1 if oy == 0 else 1))
                side_a = outside.copy()
                side_b = outside.copy()
                corner = outside.copy()
                side_a[axes[0][0]] += axes[0][1]
                side_b[axes[1][0]] += axes[1][1]
                corner[axes[0][0]] += axes[0][1]
                corner[axes[1][0]] += axes[1][1]
                a = occupied_at(side_a[0], side_a[1], side_a[2])
                b = occupied_at(side_b[0], side_b[1], side_b[2])
                c = occupied_at(corner[0], corner[1], corner[2])
                ao = 0 if a and b else 3 - int(a) - int(b) - int(c)
                ao_levels.append(ao)
                shade = face_shades[(dx, dy, dz)] * (0.60 + ao * 0.1333)
                colors.append(shade_srgb(color, shade))
            if ao_levels[0] + ao_levels[2] > ao_levels[1] + ao_levels[3]:
                faces.extend(((base, base + 1, base + 3), (base + 1, base + 2, base + 3)))
            else:
                faces.extend(((base, base + 1, base + 2), (base, base + 2, base + 3)))

    mesh = trimesh.Trimesh(
        vertices=np.asarray(vertices),
        faces=np.asarray(faces),
        vertex_normals=np.asarray(normals),
        process=False,
    )
    mesh.visual.vertex_colors = np.asarray(colors, dtype=np.uint8)
    scene = trimesh.Scene(mesh)
    path.write_bytes(export_glb_with_material(scene, material_name, quantize_normals=True))
    print(f"{source.name}: {len(occupied):,} voxels, {len(faces):,} triangles -> {path}")
    return coarse


def build_collision_course(
    coarse: np.ndarray,
    path: Path,
    factor: int,
    scale: float,
    start: tuple[int, int],
    finish: tuple[int, int],
    start_y: int | None,
    *,
    course_id: str = "salto",
    asset_name: str = "salto",
    yaw: int = 180,
    route: list[dict] | None = None,
    metadata: dict | None = None,
) -> None:
    """Emit merged 3D collision cuboids aligned to the visual GLB."""
    ch, cl, cw = coarse.shape
    # Decorative/non-colliding legacy IDs. Shape-specific blocks such as stairs,
    # slabs, fences, and panes remain conservative full-cube proxies for now.
    non_solid = [0, 6, 8, 9, 10, 11, 31, 32, 37, 38, 39, 40, 50, 51, 55, 59, 63, 65, 66, 68, 69, 75, 76, 77, 78, 83, 106, 131, 132, 143, 166, 171, 175, 176, 177]
    block_ids = coarse >> 4
    solid = ~np.isin(block_ids, non_solid)

    start_cx, start_cz = start[0] // factor, start[1] // factor
    finish_cx, finish_cz = finish[0] // factor, finish[1] // factor
    occupied_at_start = np.flatnonzero(solid[:, start_cz, start_cx])
    if len(occupied_at_start) == 0:
        raise ValueError("start coordinate has no solid surface")
    start_top = start_y // factor if start_y is not None else int(occupied_at_start[-1])

    if yaw not in {0, 90, 180, 270}:
        raise ValueError("yaw must be 0, 90, 180, or 270 degrees")

    def rotate(raw_x: float, raw_z: float) -> tuple[float, float]:
        if yaw == 0:
            return raw_x, raw_z
        if yaw == 90:
            return raw_z, -raw_x
        if yaw == 180:
            return -raw_x, -raw_z
        return -raw_z, raw_x

    start_raw_x = start_cx + 0.5 - cw / 2
    start_raw_z = start_cz + 0.5
    start_rotated_x, start_rotated_z = rotate(start_raw_x, start_raw_z)
    transform = {
        "scale": scale,
        "x": -start_rotated_x * scale,
        "y": -(start_top + 1) * scale,
        "z": -start_rotated_z * scale,
        "yaw": yaw,
    }
    death_y = -18.0

    def world_point(raw_x: float, raw_y: float, raw_z: float, *, surface: bool = True) -> dict[str, float]:
        rotated_x, rotated_z = rotate(raw_x / factor + 0.5 - cw / 2, raw_z / factor + 0.5)
        y_offset = 1 if surface else 0.5
        return {
            "x": round(transform["x"] + rotated_x * scale, 4),
            "y": round(transform["y"] + (raw_y / factor + y_offset) * scale, 4),
            "z": round(transform["z"] + rotated_z * scale, 4),
        }

    finish_y_values = np.flatnonzero(solid[:, finish_cz, finish_cx])
    finish_top = int(finish_y_values[-1]) if len(finish_y_values) else start_top
    finish_world = world_point(finish[0], finish_top * factor, finish[1])

    # Greedily merge solid voxels into 3D boxes. This preserves caves, foliage,
    # overheads, and walls instead of turning every top pixel into a solid pillar.
    visited = np.zeros_like(solid, dtype=bool)
    cuboids: list[list[int]] = []
    for y_value, z_value, x_value in np.argwhere(solid):
        y, z, x = int(y_value), int(z_value), int(x_value)
        if visited[y, z, x]:
            continue
        x1 = x + 1
        while x1 < cw and solid[y, z, x1] and not visited[y, z, x1]:
            x1 += 1
        z1 = z + 1
        while z1 < cl and np.all(solid[y, z1, x:x1] & ~visited[y, z1, x:x1]):
            z1 += 1
        y1 = y + 1
        while y1 < ch and np.all(solid[y1, z:z1, x:x1] & ~visited[y1, z:z1, x:x1]):
            y1 += 1
        visited[y:y1, z:z1, x:x1] = True
        cuboids.append([x, y, z, x1 - x, y1 - y, z1 - z, int(block_ids[y, z, x])])

    def kind(block_id: int) -> str:
        if block_id in {87, 88, 112, 173}:
            return "basalt"
        if block_id in {41, 42, 57, 133, 138}:
            return "finish"
        if block_id in {51, 89, 124}:
            return "ember"
        return "sandstone"

    blocks = []
    for index, (x, y, z, width, height, depth, block_id) in enumerate(cuboids):
        rotated_x, rotated_z = rotate(x + width / 2 - cw / 2, z + depth / 2)
        center_x = transform["x"] + rotated_x * scale
        center_z = transform["z"] + rotated_z * scale
        center_y = transform["y"] + (y + height / 2) * scale
        size_x = (depth if yaw in {90, 270} else width) * scale
        size_z = (width if yaw in {90, 270} else depth) * scale
        endpoint_distance = min(
            (center_x ** 2 + center_z ** 2) ** 0.5,
            ((center_x - finish_world["x"]) ** 2 + (center_z - finish_world["z"]) ** 2) ** 0.5,
        )
        blocks.append({
            "id": f"{course_id}-{index}",
            "x": round(center_x, 4),
            "y": round(center_y, 4),
            "z": round(center_z, 4),
            "sx": round(size_x, 4),
            "sy": round(height * scale, 4),
            "sz": round(size_z, 4),
            "kind": kind(block_id),
            "breakable": endpoint_distance >= 8,
            "visible": False,
        })

    route_payload = []
    for waypoint in route or []:
        point = world_point(waypoint["x"], waypoint["y"], waypoint["z"])
        point["label"] = waypoint["label"]
        route_payload.append(point)
    if not route_payload:
        route_payload = [
            {"x": 0, "y": 0.05, "z": 0, "label": "Launch"},
            {**finish_world, "label": "Sanctuary"},
        ]

    payload = {
        "id": course_id,
        "name": (metadata or {}).get("name", course_id.title()),
        "description": (metadata or {}).get("description", "Imported prototype route"),
        "difficulty": (metadata or {}).get("difficulty", "TEST ROUTE"),
        "music": (metadata or {}).get("music", "/music/embers-at-your-heels.mid"),
        "model": f"/models/{asset_name}.glb",
        "modelTransform": transform,
        "deathY": death_y,
        "spawn": {"x": 0, "y": 0.05, "z": 0},
        "finish": {**finish_world, "radius": 5.5},
        "route": route_payload,
        "blocks": blocks,
    }
    path.write_text(json.dumps(payload, separators=(",", ":")) + "\n")
    print(f"{course_id} collision: {len(cuboids):,} merged cuboids -> {path}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("schematic", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--factor", type=int, default=3)
    parser.add_argument("--asset-name")
    parser.add_argument("--course-output", type=Path)
    parser.add_argument("--scale", type=float, default=1.0)
    parser.add_argument("--start", nargs=2, type=int, metavar=("X", "Z"))
    parser.add_argument("--start-y", type=int, help="solid block Y under the spawn point")
    parser.add_argument("--finish", nargs=2, type=int, metavar=("X", "Z"))
    parser.add_argument("--yaw", type=int, default=180, choices=(0, 90, 180, 270))
    parser.add_argument("--course-id")
    parser.add_argument("--route-file", type=Path)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    build_player(args.output / "runner.glb")
    build_dragon(args.output / "cinder-wyrm.glb")
    asset_name = args.asset_name or args.schematic.stem.lower().replace(" ", "-")
    route_config = json.loads(args.route_file.read_text()) if args.route_file else {}
    course_id = args.course_id or asset_name
    coarse = build_schematic(args.schematic, args.output / f"{asset_name}.glb", args.factor, f"{route_config.get('name', course_id.title())} voxel terrain")
    if args.course_output:
        if not args.start or not args.finish:
            parser.error("--course-output requires --start X Z and --finish X Z")
        build_collision_course(
            coarse,
            args.course_output,
            args.factor,
            args.scale,
            tuple(args.start),
            tuple(args.finish),
            args.start_y,
            course_id=course_id,
            asset_name=asset_name,
            yaw=args.yaw,
            route=route_config.get("route"),
            metadata=route_config,
        )


if __name__ == "__main__":
    main()
