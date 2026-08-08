export type BlockKind = "basalt" | "sandstone" | "ember" | "finish";

export type CourseBlock = {
  id: string;
  x: number;
  y: number;
  z: number;
  sx: number;
  sy: number;
  sz: number;
  kind: BlockKind;
  breakable: boolean;
};

export type Checkpoint = { z: number; label: string };

const blocks: CourseBlock[] = [];
let blockId = 0;

function block(
  x: number,
  y: number,
  z: number,
  sx: number,
  sy: number,
  sz: number,
  kind: BlockKind = "sandstone",
  breakable = true,
) {
  blocks.push({ id: `b${blockId++}`, x, y, z, sx, sy, sz, kind, breakable });
}

// Start dais and the first readable jumps.
block(0, -1, 5, 10, 2, 18, "basalt", false);
block(-2.2, 0, 17, 3, 1, 4);
block(2.2, 0.8, 23, 3, 1, 4);
block(0, 1.5, 29, 2.5, 1, 3);

// A thin bridge with missing teeth. Destruction here changes the usable line.
for (let z = 34; z <= 58; z += 4) {
  if (z === 42 || z === 54) continue;
  block(z % 8 === 2 ? -1.2 : 1.2, 1.5, z, 2.5, 1, 3.2, "sandstone", true);
}
block(-3.4, 2.2, 43, 2.2, 1, 2.2, "ember", true);
block(3.4, 2.2, 54, 2.2, 1, 2.2, "ember", true);

// The switchback ruins.
block(0, 1.2, 63, 8, 1, 5, "basalt", false);
block(-4, 2.2, 69, 3, 1, 4);
block(-1, 3.2, 75, 3, 1, 4);
block(3, 4.2, 81, 3, 1, 4);
block(0, 4.2, 88, 2, 1, 6, "ember", true);
block(-3.2, 4.2, 95, 2.2, 1, 3);
block(2.8, 3.2, 101, 2.2, 1, 3);

// Final collapsing spine and sanctuary.
for (let z = 107; z <= 132; z += 5) {
  block(Math.sin(z) * 1.6, 2.2, z, 2.6, 1, 3.4, z % 10 ? "sandstone" : "ember", true);
}
block(0, 1.2, 141, 11, 1, 12, "finish", false);

// Decorative cliff ribs that also prevent easy side-skips at the start.
for (let z = 0; z < 146; z += 12) {
  block(-10, -1.5 + (z % 24) / 24, z, 6, 7, 8, "basalt", false);
  block(10, -1.5 + ((z + 12) % 24) / 24, z, 6, 7, 8, "basalt", false);
}

export const COURSE = {
  id: "ashen-causeway",
  name: "Ashen Causeway",
  finishZ: 137,
  deathY: -8,
  spawn: { x: 0, y: 0.05, z: 0 },
  blocks,
  checkpoints: [
    { z: 0, label: "The Gate" },
    { z: 31, label: "Broken Teeth" },
    { z: 61, label: "The Ruins" },
    { z: 104, label: "Dragonspine" },
    { z: 137, label: "Sanctuary" },
  ] satisfies Checkpoint[],
};

export function progressAt(z: number) {
  return Math.max(0, Math.min(100, Math.round((z / COURSE.finishZ) * 100)));
}
