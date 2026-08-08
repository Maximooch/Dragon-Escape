import type { CourseBlock } from "./course";

export function createCollisionIndex(blocks: CourseBlock[], cellSize = 4) {
  const cells = new Map<string, CourseBlock[]>();
  const key = (x: number, z: number) => `${x}:${z}`;

  for (const block of blocks) {
    const minX = Math.floor((block.x - block.sx / 2) / cellSize);
    const maxX = Math.floor((block.x + block.sx / 2) / cellSize);
    const minZ = Math.floor((block.z - block.sz / 2) / cellSize);
    const maxZ = Math.floor((block.z + block.sz / 2) / cellSize);
    for (let z = minZ; z <= maxZ; z++) {
      for (let x = minX; x <= maxX; x++) {
        const cell = key(x, z);
        const contents = cells.get(cell) ?? [];
        contents.push(block);
        cells.set(cell, contents);
      }
    }
  }

  return {
    nearby(position: { x: number; z: number }) {
      const centerX = Math.floor(position.x / cellSize);
      const centerZ = Math.floor(position.z / cellSize);
      const found = new Set<CourseBlock>();
      for (let z = centerZ - 1; z <= centerZ + 1; z++) {
        for (let x = centerX - 1; x <= centerX + 1; x++) {
          for (const block of cells.get(key(x, z)) ?? []) found.add(block);
        }
      }
      return [...found];
    },
  };
}
