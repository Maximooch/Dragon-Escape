import assert from "node:assert/strict";
import test from "node:test";
import { COURSE, COURSES, courseProgressAt, pathPointAt, progressAt } from "../lib/course";
import { createRunner, destroyNearDragon, dragonPosition, movementBasis, simulateRunner } from "../lib/simulation";

test("movement basis matches the first-person camera handedness", () => {
  const facingForward = movementBasis(0);
  assert.deepEqual(facingForward, { forwardX: 0, forwardZ: 1, rightX: -1, rightZ: 0 });
  const facingRight = movementBasis(-Math.PI / 2);
  assert.ok(Math.abs(facingRight.forwardX + 1) < 1e-10);
  assert.ok(Math.abs(facingRight.rightZ + 1) < 1e-10);
});

test("course has a complete authored race path", () => {
  for (const course of COURSES) {
    assert.equal(course.checkpoints.at(0)?.progress, 0, `${course.name} starts at zero`);
    assert.equal(course.checkpoints.at(-1)?.progress, course.finishZ, `${course.name} reaches its route length`);
    assert.ok(course.blocks.length >= 30);
    assert.ok(course.blocks.some((block) => block.breakable));
    assert.ok(course.blocks.some((block) => !block.breakable));
    assert.deepEqual(pathPointAt(course, course.finishZ), course.botPath.at(-1));
  }
});

test("progress is clamped and reaches 100 at sanctuary", () => {
  assert.equal(progressAt(-20), 0);
  assert.equal(progressAt(COURSE.finishZ), 100);
  assert.equal(progressAt(COURSE.finishZ + 100), 100);
});

test("dragon accelerates and destroys only authored breakable terrain", () => {
  assert.ok(dragonPosition(30) - dragonPosition(20) > dragonPosition(20) - dragonPosition(10));
  const destroyed = new Set<string>();
  const target = COURSE.blocks.find((block) => {
    if (!block.breakable) return false;
    const point = pathPointAt(COURSE, courseProgressAt(block, COURSE));
    return Math.hypot(block.x - point.x, block.z - point.z) < 4;
  })!;
  destroyNearDragon(destroyed, courseProgressAt(target, COURSE));
  assert.ok(destroyed.has(target.id));
  for (const id of destroyed) assert.equal(COURSE.blocks.find((block) => block.id === id)?.breakable, true);
});

test("runner moves forward and leap enters cooldown", () => {
  const runner = createRunner();
  const destroyed = new Set<string>();
  for (let i = 0; i < 30; i++) {
    simulateRunner(runner, {
      forward: true, back: false, left: false, right: false,
      jump: false, sprint: true, leap: i === 0, yaw: 0,
    }, 1 / 60, i / 60, destroyed);
  }
  assert.ok(runner.position.z > 2);
  assert.equal(runner.leapReadyAt, 8);
});
