import assert from "node:assert/strict";
import test from "node:test";
import { createCollisionIndex } from "../lib/collision";
import { COURSE } from "../lib/course";

test("Salto collision is spatially indexed around the runner", () => {
  const index = createCollisionIndex(COURSE.blocks);
  const nearby = index.nearby(COURSE.spawn);
  assert.ok(nearby.length > 0);
  assert.ok(nearby.length < COURSE.blocks.length / 20);

  const support = nearby.find((block) => {
    const top = block.y + block.sy / 2;
    return Math.abs(top - COURSE.spawn.y) < 0.1 &&
      COURSE.spawn.x >= block.x - block.sx / 2 && COURSE.spawn.x <= block.x + block.sx / 2 &&
      COURSE.spawn.z >= block.z - block.sz / 2 && COURSE.spawn.z <= block.z + block.sz / 2;
  });
  assert.ok(support, "spawn must stand on converted Salto geometry");
});
