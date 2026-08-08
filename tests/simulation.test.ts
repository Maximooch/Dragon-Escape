import assert from "node:assert/strict";
import test from "node:test";
import { COURSE, progressAt } from "../lib/course";
import { createRunner, destroyNearDragon, dragonPosition, simulateRunner } from "../lib/simulation";

test("course has a complete authored race path", () => {
  assert.equal(COURSE.checkpoints.at(0)?.z, 0);
  assert.equal(COURSE.checkpoints.at(-1)?.z, COURSE.finishZ);
  assert.ok(COURSE.blocks.length >= 30);
  assert.ok(COURSE.blocks.some((block) => block.breakable));
  assert.ok(COURSE.blocks.some((block) => !block.breakable));
});

test("progress is clamped and reaches 100 at sanctuary", () => {
  assert.equal(progressAt(-20), 0);
  assert.equal(progressAt(COURSE.finishZ), 100);
  assert.equal(progressAt(COURSE.finishZ + 100), 100);
});

test("dragon accelerates and destroys only authored breakable terrain", () => {
  assert.ok(dragonPosition(30) - dragonPosition(20) > dragonPosition(20) - dragonPosition(10));
  const destroyed = new Set<string>();
  const target = COURSE.blocks.find((block) => block.breakable)!;
  destroyNearDragon(destroyed, target.z);
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
