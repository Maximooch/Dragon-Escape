import assert from "node:assert/strict";
import test from "node:test";
import { createCollisionIndex } from "../lib/collision";
import { COURSES } from "../lib/course";

test("every course has spatially indexed support under its spawn", () => {
  for (const course of COURSES) {
    const index = createCollisionIndex(course.blocks);
    const nearby = index.nearby(course.spawn);
    assert.ok(nearby.length > 0, `${course.name} has nearby collision`);
    assert.ok(nearby.length < course.blocks.length / 10, `${course.name} collision query stays sparse`);

    const support = nearby.find((block) => {
      const top = block.y + block.sy / 2;
      return Math.abs(top - course.spawn.y) < 0.1 &&
        course.spawn.x >= block.x - block.sx / 2 && course.spawn.x <= block.x + block.sx / 2 &&
        course.spawn.z >= block.z - block.sz / 2 && course.spawn.z <= block.z + block.sz / 2;
    });
    assert.ok(support, `${course.name} spawn must stand on converted geometry`);
  }
});
