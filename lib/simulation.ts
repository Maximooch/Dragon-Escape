import { COURSE, courseProgressAt, pathPointAt, type CourseBlock, type CourseDefinition } from "./course";
import { createCollisionIndex } from "./collision";

export type Vec3 = { x: number; y: number; z: number };
export type InputState = {
  forward: boolean;
  back: boolean;
  left: boolean;
  right: boolean;
  jump: boolean;
  sprint: boolean;
  leap: boolean;
  yaw: number;
};
export type RunnerState = {
  position: Vec3;
  velocity: Vec3;
  grounded: boolean;
  alive: boolean;
  finished: boolean;
  checkpoint: number;
  leapReadyAt: number;
};

export const EMPTY_INPUT: InputState = {
  forward: false, back: false, left: false, right: false,
  jump: false, sprint: false, leap: false, yaw: 0,
};

export function movementBasis(yaw: number) {
  return {
    forwardX: Math.sin(yaw),
    forwardZ: Math.cos(yaw),
    rightX: -Math.cos(yaw),
    rightZ: Math.sin(yaw),
  };
}

export function createRunner(course: CourseDefinition = COURSE): RunnerState {
  return {
    position: { ...course.spawn },
    velocity: { x: 0, y: 0, z: 0 },
    grounded: false,
    alive: true,
    finished: false,
    checkpoint: 0,
    leapReadyAt: 0,
  };
}

const radius = 0.34;
const height = 1.72;
const collisionIndexes = new WeakMap<CourseDefinition, ReturnType<typeof createCollisionIndex>>();

function collisionIndex(course: CourseDefinition) {
  const existing = collisionIndexes.get(course);
  if (existing) return existing;
  const created = createCollisionIndex(course.blocks);
  collisionIndexes.set(course, created);
  return created;
}

function overlap(position: Vec3, b: CourseBlock) {
  return position.x + radius > b.x - b.sx / 2 &&
    position.x - radius < b.x + b.sx / 2 &&
    position.y + height > b.y - b.sy / 2 &&
    position.y < b.y + b.sy / 2 &&
    position.z + radius > b.z - b.sz / 2 &&
    position.z - radius < b.z + b.sz / 2;
}

function blocked(position: Vec3, destroyed: ReadonlySet<string>, course: CourseDefinition) {
  return collisionIndex(course).nearby(position).some((b) => !destroyed.has(b.id) && overlap(position, b));
}

export function simulateRunner(
  runner: RunnerState,
  input: InputState,
  dt: number,
  now: number,
  destroyed: ReadonlySet<string>,
  course: CourseDefinition = COURSE,
) {
  if (!runner.alive || runner.finished) return;

  const yaw = input.yaw;
  const { forwardX, forwardZ, rightX, rightZ } = movementBasis(yaw);
  let moveX = (input.forward ? 1 : 0) - (input.back ? 1 : 0);
  let moveY = (input.right ? 1 : 0) - (input.left ? 1 : 0);
  const length = Math.hypot(moveX, moveY) || 1;
  moveX /= length;
  moveY /= length;
  const speed = input.sprint ? 9.2 : 7.1;
  const targetX = (forwardX * moveX + rightX * moveY) * speed;
  const targetZ = (forwardZ * moveX + rightZ * moveY) * speed;
  const control = runner.grounded ? 22 : 6;
  runner.velocity.x += (targetX - runner.velocity.x) * Math.min(1, control * dt);
  runner.velocity.z += (targetZ - runner.velocity.z) * Math.min(1, control * dt);

  if (input.jump && runner.grounded) {
    runner.velocity.y = 7.9;
    runner.grounded = false;
  }
  if (input.leap && now >= runner.leapReadyAt) {
    runner.velocity.x += forwardX * 9.5;
    runner.velocity.z += forwardZ * 9.5;
    runner.velocity.y = Math.max(runner.velocity.y, 4.4);
    runner.leapReadyAt = now + 8;
  }
  runner.velocity.y -= 18.5 * dt;

  const previousY = runner.position.y;
  const nextX = { ...runner.position, x: runner.position.x + runner.velocity.x * dt };
  if (!blocked(nextX, destroyed, course)) runner.position.x = nextX.x;
  else runner.velocity.x = 0;

  const nextZ = { ...runner.position, z: runner.position.z + runner.velocity.z * dt };
  if (!blocked(nextZ, destroyed, course)) runner.position.z = nextZ.z;
  else runner.velocity.z = 0;

  runner.grounded = false;
  const nextY = { ...runner.position, y: runner.position.y + runner.velocity.y * dt };
  if (!blocked(nextY, destroyed, course)) {
    runner.position.y = nextY.y;
  } else if (runner.velocity.y <= 0) {
    let bestTop = -Infinity;
    for (const b of collisionIndex(course).nearby(runner.position)) {
      if (destroyed.has(b.id)) continue;
      const top = b.y + b.sy / 2;
      const horizontallyInside = runner.position.x + radius > b.x - b.sx / 2 &&
        runner.position.x - radius < b.x + b.sx / 2 &&
        runner.position.z + radius > b.z - b.sz / 2 &&
        runner.position.z - radius < b.z + b.sz / 2;
      if (horizontallyInside && previousY >= top - 0.2 && top > bestTop) bestTop = top;
    }
    if (bestTop > -Infinity) {
      runner.position.y = bestTop + 0.002;
      runner.velocity.y = 0;
      runner.grounded = true;
    }
  } else {
    runner.velocity.y = 0;
  }

  const routeProgress = courseProgressAt(runner.position, course);
  const checkpoints = course.checkpoints;
  while (runner.checkpoint + 1 < checkpoints.length && routeProgress >= checkpoints[runner.checkpoint + 1].progress - 0.75) {
    runner.checkpoint += 1;
  }
  const finish = course.finish;
  if (Math.hypot(runner.position.x - finish.x, runner.position.z - finish.z) <= finish.radius &&
    Math.abs(runner.position.y - finish.y) <= finish.radius) runner.finished = true;
  if (runner.position.y < course.deathY) runner.alive = false;
}

export function dragonPosition(elapsed: number) {
  const delay = 5;
  if (elapsed < delay) return -16;
  const t = elapsed - delay;
  return -16 + t * 4.35 + t * t * 0.018;
}

export function destroyNearDragon(destroyed: Set<string>, dragonZ: number, course: CourseDefinition = COURSE) {
  const dragon = pathPointAt(course, dragonZ);
  for (const b of course.blocks) {
    if (!b.breakable || destroyed.has(b.id)) continue;
    if (Math.hypot(b.x - dragon.x, b.z - dragon.z) < 4.4) destroyed.add(b.id);
  }
}

export function respawnAtCheckpoint(runner: RunnerState, course: CourseDefinition = COURSE) {
  const checkpoint = course.checkpoints[Math.max(0, runner.checkpoint - 1)];
  runner.position = { x: checkpoint.x, y: checkpoint.y + 2, z: checkpoint.z };
  runner.velocity = { x: 0, y: 0, z: 0 };
  runner.alive = true;
}
