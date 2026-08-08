import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

for (const name of ["runner.glb", "cinder-wyrm.glb", "grumble-volcano.glb"]) {
  test(`${name} is a valid binary glTF asset`, async () => {
    const data = await readFile(new URL(`../public/models/${name}`, import.meta.url));
    assert.equal(data.subarray(0, 4).toString("ascii"), "glTF");
    assert.equal(data.readUInt32LE(4), 2);
    assert.equal(data.readUInt32LE(8), data.length);
  });
}
