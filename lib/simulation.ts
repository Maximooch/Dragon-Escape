import { COURSE, type CourseBlock } from "./course";

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

export function createRunner(): RunnerState {
  return {
    position: { ...COURSE.spawn },
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

function overlap(position: Vec3, b: CourseBlock) {
  return position.x + radius > b.x - b.sx / 2 &&
    position.x - radius < b.x + b.sx / 2 &&
    position.y + height > b.y - b.sy / 2 &&
    position.y < b.y + b.sy / 2 &&
    position.z + radius > b.z - b.sz / 2 &&
    position.z - radius < b.z + b.sz / 2;
}

function blocked(position: Vec3, destroyed: ReadonlySet<string>) {
  return COURSE.blocks.some((b) => !destroyed.has(b.id) && overlap(position, b));
}

export function simulateRunner(
  runner: RunnerState,
  input: InputState,
  dt: number,
  now: number,
  destroyed: ReadonlySet<string>,
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
  if (!blocked(nextX, destroyed)) runner.position.x = nextX.x;
  else runner.velocity.x = 0;

  const nextZ = { ...runner.position, z: runner.position.z + runner.velocity.z * dt };
  if (!blocked(nextZ, destroyed)) runner.position.z = nextZ.z;
  else runner.velocity.z = 0;

  runner.grounded = false;
  const nextY = { ...runner.position, y: runner.position.y + runner.velocity.y * dt };
  if (!blocked(nextY, destroyed)) {
    runner.position.y = nextY.y;
  } else if (runner.velocity.y <= 0) {
    let bestTop = -Infinity;
    for (const b of COURSE.blocks) {
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

  const checkpoints = COURSE.checkpoints;
  while (runner.checkpoint + 1 < checkpoints.length && runner.position.z >= checkpoints[runner.checkpoint + 1].z) {
    runner.checkpoint += 1;
  }
  if (runner.position.z >= COURSE.finishZ && runner.position.y > -1) runner.finished = true;
  if (runner.position.y < COURSE.deathY) runner.alive = false;
}

export function dragonPosition(elapsed: number) {
  const delay = 5;
  if (elapsed < delay) return -16;
  const t = elapsed - delay;
  return -16 + t * 4.35 + t * t * 0.018;
}

export function destroyNearDragon(destroyed: Set<string>, dragonZ: number) {
  for (const b of COURSE.blocks) {
    if (!b.breakable || destroyed.has(b.id)) continue;
    const dz = b.z - dragonZ;
    if (Math.abs(dz) < 3.7) destroyed.add(b.id);
  }
}

export function respawnAtCheckpoint(runner: RunnerState) {
  const checkpoint = COURSE.checkpoints[Math.max(0, runner.checkpoint - 1)];
  runner.position = { x: 0, y: 7, z: checkpoint.z };
  runner.velocity = { x: 0, y: 0, z: 0 };
  runner.alive = true;
}
