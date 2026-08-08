import archipelCourse from "./archipel-course.generated.json";
import cavesCourse from "./caves-course.generated.json";
import frostRunCourse from "./frost-run-course.generated.json";
import saltoCourse from "./salto-course.generated.json";

export type BlockKind = "basalt" | "sandstone" | "ember" | "finish";
export type Vec3 = { x: number; y: number; z: number };

export type CourseBlock = Vec3 & {
  id: string;
  sx: number;
  sy: number;
  sz: number;
  kind: BlockKind;
  breakable: boolean;
  visible?: boolean;
};

export type Checkpoint = Vec3 & { label: string; progress: number };
export type CourseOption = {
  id: string;
  name: string;
  description: string;
  difficulty: string;
  music: string;
  status: "PLAYABLE" | "PROTOTYPE ROUTE";
};

export type CourseDefinition = CourseOption & {
  model: string;
  modelTransform: { scale: number; x: number; y: number; z: number; yaw: number };
  deathY: number;
  spawn: Vec3;
  finish: Vec3 & { radius: number };
  finishZ: number;
  blocks: CourseBlock[];
  checkpoints: Checkpoint[];
  botPath: Vec3[];
  pathDistances: number[];
};

type GeneratedCourseData = {
  id: string;
  name: string;
  description: string;
  difficulty: string;
  music: string;
  model: string;
  modelTransform: CourseDefinition["modelTransform"];
  deathY: number;
  spawn: Vec3;
  finish: Vec3 & { radius: number };
  route: Array<Vec3 & { label: string }>;
  blocks: CourseBlock[];
};

type SaltoCourseData = {
  model: string;
  modelTransform: CourseDefinition["modelTransform"];
  finishZ: number;
  deathY: number;
  spawn: Vec3;
  blocks: CourseBlock[];
};

function distances(path: Vec3[]) {
  const result = [0];
  for (let index = 1; index < path.length; index += 1) {
    const previous = path[index - 1];
    const point = path[index];
    result.push(result[index - 1] + Math.hypot(point.x - previous.x, point.y - previous.y, point.z - previous.z));
  }
  return result;
}

function compileGenerated(raw: GeneratedCourseData): CourseDefinition {
  const botPath = raw.route.map(({ x, y, z }) => ({ x, y, z }));
  const pathDistances = distances(botPath);
  const finishZ = pathDistances.at(-1) ?? 1;
  return {
    id: raw.id,
    name: raw.name,
    description: raw.description,
    difficulty: raw.difficulty,
    music: raw.music,
    status: "PROTOTYPE ROUTE",
    model: raw.model,
    modelTransform: raw.modelTransform,
    deathY: raw.deathY,
    spawn: raw.spawn,
    finish: { ...raw.finish, ...botPath.at(-1), radius: raw.finish.radius },
    finishZ,
    blocks: raw.blocks,
    botPath,
    pathDistances,
    checkpoints: raw.route.map((point, index) => ({ ...point, progress: pathDistances[index] })),
  };
}

const saltoData = saltoCourse as SaltoCourseData;
const saltoBotPath: Vec3[] = [
  { x: 0, y: 0, z: 0 }, { x: -4.3, y: 0.9, z: 21.5 }, { x: 1.7, y: 0.9, z: 27.5 },
  { x: -2.6, y: 2.6, z: 35.3 }, { x: 0.9, y: 8.6, z: 56.8 }, { x: 5.2, y: 9.5, z: 61.9 },
  { x: 9.5, y: 10.3, z: 66.2 }, { x: 13.8, y: 11.2, z: 70.5 }, { x: 23.2, y: 12.9, z: 78.3 },
  { x: 44.7, y: 16.3, z: 79.5 }, { x: 64.5, y: 19.8, z: 80 }, { x: 70.5, y: 28.4, z: 82 },
  { x: 78.3, y: 26.7, z: 93.7 }, { x: 82.6, y: 24.9, z: 100.6 }, { x: 92, y: 23.2, z: 112.7 },
  { x: 90.3, y: 27.5, z: 133.3 }, { x: 95.5, y: 27.5, z: 138.5 }, { x: 90.3, y: 27.5, z: 143.6 },
  { x: 76.5, y: 27.5, z: 147.1 }, { x: 74.8, y: 28.4, z: 155.7 }, { x: 66.2, y: 43, z: 160 },
  { x: 66.2, y: 36.1, z: 187.5 }, { x: 70.5, y: 37.8, z: 196.1 }, { x: 66.2, y: 37.8, z: 204.7 },
  { x: 68.8, y: 47.3, z: saltoData.finishZ },
];
const saltoDistances = distances(saltoBotPath);
const saltoCheckpointPoints = [
  { x: 0, y: 0, z: 0, label: "Coalheart Launch" },
  { x: -2.6, y: 2.6, z: 35.3, label: "The Lower Isles" },
  { x: 13.8, y: 11.2, z: 70.5, label: "Splitstone Crossing" },
  { x: 92, y: 23.2, z: 112.7, label: "The High Steps" },
  { x: 74.8, y: 28.4, z: 155.7, label: "Village Ascent" },
  { x: 70.5, y: 37.8, z: 196.1, label: "Final Climb" },
  { x: 68.8, y: 47.3, z: saltoData.finishZ, label: "Beacon Crown" },
];

function nearestPathDistance(point: Vec3, path: Vec3[], pathDistances: number[]) {
  let nearest = { distanceSquared: Infinity, progress: 0 };
  for (let index = 1; index < path.length; index += 1) {
    const start = path[index - 1];
    const end = path[index];
    const dx = end.x - start.x;
    const dy = end.y - start.y;
    const dz = end.z - start.z;
    const lengthSquared = dx * dx + dy * dy + dz * dz || 1;
    const t = Math.max(0, Math.min(1, ((point.x - start.x) * dx + (point.y - start.y) * dy + (point.z - start.z) * dz) / lengthSquared));
    const px = start.x + dx * t;
    const py = start.y + dy * t;
    const pz = start.z + dz * t;
    const distanceSquared = (point.x - px) ** 2 + (point.y - py) ** 2 * 0.35 + (point.z - pz) ** 2;
    if (distanceSquared < nearest.distanceSquared) {
      nearest = {
        distanceSquared,
        progress: pathDistances[index - 1] + (pathDistances[index] - pathDistances[index - 1]) * t,
      };
    }
  }
  return nearest.progress;
}

export const COURSE: CourseDefinition = {
  id: "salto",
  name: "Salto",
  description: "Climb a collapsing sandstone archipelago toward the cyan beacon crown.",
  difficulty: "VERTICAL / CLASSIC",
  music: "/music/embers-at-your-heels.mid",
  status: "PLAYABLE",
  model: saltoData.model,
  modelTransform: saltoData.modelTransform,
  deathY: saltoData.deathY,
  spawn: saltoData.spawn,
  finish: { x: 68.8, y: 47.3, z: saltoData.finishZ, radius: 5.5 },
  finishZ: saltoDistances.at(-1) ?? saltoData.finishZ,
  blocks: saltoData.blocks,
  botPath: saltoBotPath,
  pathDistances: saltoDistances,
  checkpoints: saltoCheckpointPoints.map((point) => ({ ...point, progress: nearestPathDistance(point, saltoBotPath, saltoDistances) })),
};

const catalog = [
  COURSE,
  compileGenerated(cavesCourse as GeneratedCourseData),
  compileGenerated(frostRunCourse as GeneratedCourseData),
  compileGenerated(archipelCourse as GeneratedCourseData),
];

export const COURSES: readonly CourseDefinition[] = catalog;
export const COURSE_OPTIONS: readonly CourseOption[] = catalog.map(({ id, name, description, difficulty, music, status }) => ({ id, name, description, difficulty, music, status }));

export function getCourse(id: string) {
  return catalog.find((course) => course.id === id) ?? COURSE;
}

export function pathPointAt(course: CourseDefinition, progress: number): Vec3 {
  const clamped = Math.max(0, Math.min(course.finishZ, progress));
  for (let index = 1; index < course.botPath.length; index += 1) {
    if (clamped <= course.pathDistances[index]) {
      const start = course.botPath[index - 1];
      const end = course.botPath[index];
      const span = Math.max(0.001, course.pathDistances[index] - course.pathDistances[index - 1]);
      const t = (clamped - course.pathDistances[index - 1]) / span;
      return { x: start.x + (end.x - start.x) * t, y: start.y + (end.y - start.y) * t, z: start.z + (end.z - start.z) * t };
    }
  }
  return { ...course.botPath.at(-1)! };
}

export function courseProgressAt(position: Vec3, course: CourseDefinition = COURSE) {
  return nearestPathDistance(position, course.botPath, course.pathDistances);
}

export function progressAt(progress: number, course: CourseDefinition = COURSE) {
  return Math.max(0, Math.min(100, Math.round((progress / course.finishZ) * 100)));
}
