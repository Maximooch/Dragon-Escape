import saltoCourse from "./salto-course.generated.json";

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
  visible?: boolean;
};

export type Checkpoint = { x: number; y: number; z: number; label: string };

type SaltoCourseData = {
  model: string;
  modelTransform: { scale: number; x: number; y: number; z: number; yaw: number };
  finishZ: number;
  deathY: number;
  spawn: { x: number; y: number; z: number };
  blocks: CourseBlock[];
};

const data = saltoCourse as SaltoCourseData;

export const COURSE = {
  id: "salto",
  name: "Salto",
  model: data.model,
  modelTransform: data.modelTransform,
  finishZ: data.finishZ,
  deathY: data.deathY,
  spawn: data.spawn,
  finish: { x: 68.8, y: 47.3, z: 219.3, radius: 5.5 },
  blocks: data.blocks,
  checkpoints: [
    { x: 0, y: 0, z: 0, label: "Coalheart Launch" },
    { x: -2.6, y: 2.6, z: 35, label: "The Lower Isles" },
    { x: 13.8, y: 11.2, z: 70, label: "Splitstone Crossing" },
    { x: 92, y: 23.2, z: 113, label: "The High Steps" },
    { x: 74.8, y: 28.4, z: 156, label: "Village Ascent" },
    { x: 70.5, y: 37.8, z: 196, label: "Final Climb" },
    { x: 68.8, y: 47.3, z: data.finishZ, label: "Beacon Crown" },
  ] satisfies Checkpoint[],
  botPath: [
    { x: 0, y: 0, z: 0 },
    { x: -4.3, y: 0.9, z: 21.5 },
    { x: 1.7, y: 0.9, z: 27.5 },
    { x: -2.6, y: 2.6, z: 35.3 },
    { x: 0.9, y: 8.6, z: 56.8 },
    { x: 5.2, y: 9.5, z: 61.9 },
    { x: 9.5, y: 10.3, z: 66.2 },
    { x: 13.8, y: 11.2, z: 70.5 },
    { x: 23.2, y: 12.9, z: 78.3 },
    { x: 44.7, y: 16.3, z: 79.5 },
    { x: 64.5, y: 19.8, z: 80 },
    { x: 70.5, y: 28.4, z: 82 },
    { x: 78.3, y: 26.7, z: 93.7 },
    { x: 82.6, y: 24.9, z: 100.6 },
    { x: 92, y: 23.2, z: 112.7 },
    { x: 90.3, y: 27.5, z: 133.3 },
    { x: 95.5, y: 27.5, z: 138.5 },
    { x: 90.3, y: 27.5, z: 143.6 },
    { x: 76.5, y: 27.5, z: 147.1 },
    { x: 74.8, y: 28.4, z: 155.7 },
    { x: 66.2, y: 43, z: 160 },
    { x: 66.2, y: 36.1, z: 187.5 },
    { x: 70.5, y: 37.8, z: 196.1 },
    { x: 66.2, y: 37.8, z: 204.7 },
    { x: 68.8, y: 47.3, z: data.finishZ },
  ],
};

export function progressAt(z: number) {
  return Math.max(0, Math.min(100, Math.round((z / COURSE.finishZ) * 100)));
}
