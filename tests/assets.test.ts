import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

function readGlbDocument(data: Buffer) {
  const jsonLength = data.readUInt32LE(12);
  assert.equal(data.readUInt32LE(16), 0x4e4f534a);
  return JSON.parse(data.subarray(20, 20 + jsonLength).toString("utf8").trimEnd());
}

for (const name of ["runner.glb", "cinder-wyrm.glb", "salto.glb"]) {
  test(`${name} is a valid binary glTF asset`, async () => {
    const data = await readFile(new URL(`../public/models/${name}`, import.meta.url));
    assert.equal(data.subarray(0, 4).toString("ascii"), "glTF");
    assert.equal(data.readUInt32LE(4), 2);
    assert.equal(data.readUInt32LE(8), data.length);
  });
}

test("Salto GLB carries explicit colors, normals, and a terrain material", async () => {
  const data = await readFile(new URL("../public/models/salto.glb", import.meta.url));
  const document = readGlbDocument(data);
  const primitive = document.meshes[0].primitives[0];
  assert.ok("COLOR_0" in primitive.attributes);
  assert.ok("NORMAL" in primitive.attributes);
  assert.equal(document.materials[primitive.material].name, "Salto voxel terrain");
  assert.ok(document.extensionsRequired.includes("KHR_mesh_quantization"));
});

for (const [name, materialName] of [
  ["runner.glb", "Runner voxel character"],
  ["cinder-wyrm.glb", "Cinder Wyrm voxel character"],
] as const) {
  test(`${name} carries an explicit vertex-color material`, async () => {
    const data = await readFile(new URL(`../public/models/${name}`, import.meta.url));
    const document = readGlbDocument(data);
    assert.equal(document.materials[0].name, materialName);
    assert.ok(document.meshes.every((mesh: { primitives: Array<{ material?: number }> }) =>
      mesh.primitives.every((primitive) => primitive.material === 0)));
  });
}
